package pe.jamix.catalog.user;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Login against app_users. Also decides what a user may do right now (see authoritiesFor). */
@Service
public class AppUserDetailsService implements UserDetailsService {

    /** Only authority of someone who must replace a temporary password: allows /api/v1/me/** and nothing else. */
    public static final String PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED";

    private final AppUserRepository users;

    public AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    public static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public static List<@NonNull GrantedAuthority> authoritiesFor(AppUser user) {
        String authority = user.isMustChangePassword() ? PASSWORD_CHANGE_REQUIRED : user.getRole().authority();
        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) {
        AppUser user = users.findByUsername(normalize(username))
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authoritiesFor(user))
                .disabled(!user.isActive())
                .build();
    }
}
