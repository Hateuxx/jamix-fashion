package pe.jamix.catalog.user;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.jamix.catalog.auth.TokenService;
import pe.jamix.catalog.common.ConflictException;
import pe.jamix.catalog.common.NotFoundException;
import pe.jamix.catalog.user.UserDtos.CreateUserRequest;
import pe.jamix.catalog.user.UserDtos.UpdateUserRequest;
import pe.jamix.catalog.user.UserDtos.UserResponse;

@Service
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public UserService(AppUserRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    // ---------------------------------------------------------------- the current user

    @Transactional(readOnly = true)
    public TokenService.Session sessionAfterLogin(String username) {
        return tokens.issue(find(username));
    }

    @Transactional(readOnly = true)
    public UserResponse me(String username) {
        return UserResponse.from(find(username));
    }

    /** Replaces the password (also a pending temporary one) and returns a fresh session: older ones stop working. */
    @Transactional
    public TokenService.Session changeOwnPassword(String username, String currentPassword, String newPassword) {
        AppUser user = find(username);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta a la actual.");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        return tokens.issue(users.saveAndFlush(user));
    }

    // ---------------------------------------------------------------- team management (owner)

    @Transactional(readOnly = true)
    public List<UserResponse> team() {
        return users.findAll().stream()
                .sorted(Comparator.comparing((AppUser u) -> u.getRole().ordinal()).reversed()
                        .thenComparing(AppUser::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        String username = AppUserDetailsService.normalize(req.username());
        if (users.existsByUsername(username)) {
            throw new ConflictException("Ya existe el usuario '" + username + "'.");
        }
        AppUser user = new AppUser(username, req.displayName().trim(), passwordEncoder.encode(req.temporaryPassword()),
                req.role(), true, Instant.now());
        return UserResponse.from(users.save(user));
    }

    @Transactional
    public UserResponse update(long id, UpdateUserRequest req, String actingUsername) {
        AppUser user = find(id);
        boolean self = user.getUsername().equals(actingUsername);

        if (req.displayName() != null) {
            user.rename(req.displayName().trim());
        }
        if (req.role() != null && req.role() != user.getRole()) {
            if (self) {
                throw new ConflictException("No puedes cambiar tu propio rol.");
            }
            requireAnotherOwnerIfLosing(user, req.role() == Role.OWNER);
            user.changeRole(req.role());
        }
        if (req.active() != null && req.active() != user.isActive()) {
            if (self && !req.active()) {
                throw new ConflictException("No puedes desactivar tu propia cuenta.");
            }
            requireAnotherOwnerIfLosing(user, req.active());
            user.setActive(req.active());
        }
        return UserResponse.from(users.saveAndFlush(user));
    }

    @Transactional
    public UserResponse resetPassword(long id, String temporaryPassword) {
        AppUser user = find(id);
        user.resetPassword(passwordEncoder.encode(temporaryPassword));
        return UserResponse.from(users.saveAndFlush(user));
    }

    /** The shop must always keep at least one active owner, or nobody could manage the team again. */
    private void requireAnotherOwnerIfLosing(AppUser user, boolean staysOwner) {
        if (user.getRole() == Role.OWNER && user.isActive() && !staysOwner
                && users.countByRoleAndActiveTrue(Role.OWNER) <= 1) {
            throw new ConflictException("Debe quedar al menos un(a) dueño(a) activo(a).");
        }
    }

    private AppUser find(String username) {
        return users.findByUsername(username).orElseThrow(() -> new NotFoundException("Usuario no encontrado."));
    }

    private AppUser find(long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado."));
    }
}
