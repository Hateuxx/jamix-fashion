package pe.jamix.catalog.user;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.jamix.catalog.auth.TokenService;
import pe.jamix.catalog.user.UserDtos.ChangePasswordRequest;
import pe.jamix.catalog.user.UserDtos.UserResponse;

/** The logged-in person's own account. Reachable even while a password change is pending. */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserService users;

    public MeController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public UserResponse me(Authentication authentication) {
        return users.me(authentication.getName());
    }

    /** Returns a new session: the current token and any other open session stop working. */
    @PutMapping("/password")
    public TokenService.Session changePassword(Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        return users.changeOwnPassword(authentication.getName(), request.currentPassword(), request.newPassword());
    }
}
