package pe.jamix.catalog.user;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.jamix.catalog.config.JamixProperties;

/**
 * Creates the first accounts from jamix.security.users (application.yml + .env) when app_users is
 * empty. After that, the owner manages the team from the admin panel and those settings are ignored.
 */
@Component
@Order(0)
public class UserBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserBootstrap.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JamixProperties properties;

    public UserBootstrap(AppUserRepository users, PasswordEncoder passwordEncoder, JamixProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        Instant now = Instant.now();
        for (JamixProperties.User u : properties.security().users()) {
            String password = u.password();
            // An empty JAMIX_*_PASSWORD= line in .env would otherwise create a passwordless account.
            if (password == null || password.isBlank()) {
                throw new IllegalStateException("Password for user '" + u.username()
                        + "' is empty. Set it in .env (see .env.example).");
            }
            Role role = roleOf(u);
            String hash = password.startsWith("{") ? password : passwordEncoder.encode(password);
            String name = role == Role.OWNER ? "Administración" : u.username();
            users.save(new AppUser(AppUserDetailsService.normalize(u.username()), name, hash, role, false, now));
            log.info("Created initial account '{}' ({})", u.username(), role);
        }
    }

    private static Role roleOf(JamixProperties.User u) {
        String first = u.roles() == null || u.roles().isEmpty() ? "CUSTOMER" : u.roles().get(0);
        return "ADMIN".equalsIgnoreCase(first) ? Role.OWNER : Role.valueOf(first.toUpperCase());
    }
}
