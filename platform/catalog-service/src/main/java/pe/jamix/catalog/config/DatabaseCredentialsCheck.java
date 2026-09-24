package pe.jamix.catalog.config;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Stops startup before any connection attempt when the MySQL password is missing.
 *
 * Without this, an undefined DB_PASSWORD is passed to the driver as the literal text
 * "${DB_PASSWORD}", which ends in a misleading "Access denied" from the server.
 * Runs after the config files (and the optional .env import) have been loaded.
 */
public class DatabaseCredentialsCheck implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(@NonNull ConfigurableEnvironment environment,
            @NonNull SpringApplication application) {
        String url = environment.getProperty("spring.datasource.url", "");
        if (!url.startsWith("jdbc:mysql:")) {
            return; // e.g. the in-memory "h2" profile
        }
        String password = environment.getProperty("DB_PASSWORD");
        if (password == null || password.isBlank()) {
            // Reported by MissingDatabaseCredentialsFailureAnalyzer as a short setup message.
            throw new MissingDatabaseCredentialsException();
        }
        useAppAccountForMigrationsIfBlank(environment);
    }

    /**
     * "DB_MIGRATION_USERNAME=" (empty, as hosting dashboards and .env files often leave it) counts as
     * set, so the ${DB_MIGRATION_USERNAME:...} fallback in application.yml would never apply and Flyway
     * would log in with an empty user. Treat blank as "not set": migrations use the app account.
     */
    private static void useAppAccountForMigrationsIfBlank(ConfigurableEnvironment environment) {
        String migrationUser = environment.getProperty("DB_MIGRATION_USERNAME");
        if (migrationUser != null && migrationUser.isBlank()) {
            environment.getPropertySources().addFirst(new MapPropertySource("jamixBlankMigrationAccount", Map.of(
                    "spring.flyway.user", "${spring.datasource.username}",
                    "spring.flyway.password", "${spring.datasource.password}")));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // after ConfigDataEnvironmentPostProcessor has read application.yml and .env
    }
}
