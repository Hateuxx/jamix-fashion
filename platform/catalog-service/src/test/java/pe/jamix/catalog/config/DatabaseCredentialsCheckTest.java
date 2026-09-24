package pe.jamix.catalog.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/** Plain unit test: no Spring context, just the environment rules. */
class DatabaseCredentialsCheckTest {

    private static StandardEnvironment environment(Map<String, Object> values) {
        Map<String, Object> props = new HashMap<>(values);
        // The same defaults application.yml declares.
        props.putIfAbsent("spring.datasource.url", "jdbc:mysql://localhost:3306/jamix_fashion");
        props.putIfAbsent("spring.datasource.username", "jamix_app");
        props.putIfAbsent("spring.datasource.password", "${DB_PASSWORD}");
        props.putIfAbsent("spring.flyway.user", "${DB_MIGRATION_USERNAME:${spring.datasource.username}}");
        props.putIfAbsent("spring.flyway.password", "${DB_MIGRATION_PASSWORD:${spring.datasource.password}}");
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", props));
        return env;
    }

    private static void check(StandardEnvironment env) {
        new DatabaseCredentialsCheck().postProcessEnvironment(env, new SpringApplication());
    }

    @Test
    void missingPasswordStopsStartup() {
        assertThatThrownBy(() -> check(environment(Map.of()))).isInstanceOf(MissingDatabaseCredentialsException.class);
    }

    @Test
    void blankMigrationUserFallsBackToTheAppAccount() {
        StandardEnvironment env = environment(Map.of("DB_PASSWORD", "secreta", "DB_MIGRATION_USERNAME", "",
                "DB_MIGRATION_PASSWORD", ""));
        check(env);
        assertThat(env.getProperty("spring.flyway.user")).isEqualTo("jamix_app");
        assertThat(env.getProperty("spring.flyway.password")).isEqualTo("secreta");
    }

    @Test
    void explicitMigrationAccountIsKept() {
        StandardEnvironment env = environment(Map.of("DB_PASSWORD", "secreta", "DB_MIGRATION_USERNAME", "jamix_migrator",
                "DB_MIGRATION_PASSWORD", "otra"));
        check(env);
        assertThat(env.getProperty("spring.flyway.user")).isEqualTo("jamix_migrator");
        assertThat(env.getProperty("spring.flyway.password")).isEqualTo("otra");
    }

    @Test
    void inMemoryProfileNeedsNoPassword() {
        check(environment(Map.of("spring.datasource.url", "jdbc:h2:mem:jamix")));
    }
}
