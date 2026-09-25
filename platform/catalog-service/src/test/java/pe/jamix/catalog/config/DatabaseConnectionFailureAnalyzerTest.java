package pe.jamix.catalog.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.mock.env.MockEnvironment;

class DatabaseConnectionFailureAnalyzerTest {

    private static final String URL = "jdbc:mysql://localhost:3306/jamix_fashion?sslMode=REQUIRED";

    /** Mimics how the MySQL error arrives: wrapped several times by Spring/Flyway. */
    private static Throwable wrapped(SQLException sql) {
        return new IllegalStateException("Error creating bean 'entityManagerFactory'",
                new RuntimeException("Unable to obtain connection from database", sql));
    }

    private static FailureAnalysis analyze(Throwable failure, Map<String, String> systemEnv) {
        MockEnvironment env = new MockEnvironment().withProperty("spring.datasource.url", URL);
        return new DatabaseConnectionFailureAnalyzer(env, systemEnv).analyze(failure);
    }

    @Test
    void mysqlNotRunningOnThePc() {
        FailureAnalysis analysis = analyze(wrapped(new SQLException("Communications link failure", "08S01")), Map.of());
        assertThat(analysis.getDescription()).contains("No se pudo conectar a MySQL en localhost:3306");
        assertThat(analysis.getAction()).contains("net start", "services.msc", "npm run dev:h2");
    }

    @Test
    void mysqlNotReachableOnRender() {
        FailureAnalysis analysis = analyze(wrapped(new SQLException("Communications link failure", "08S01")),
                Map.of("RENDER", "true"));
        assertThat(analysis.getAction()).contains("En Render", "DB_URL").doesNotContain("net start");
    }

    @Test
    void wrongPassword() {
        FailureAnalysis analysis = analyze(wrapped(new SQLException("Access denied for user 'jamix_app'@'localhost'", "28000")), Map.of());
        assertThat(analysis.getDescription()).contains("rechazó el usuario o la contraseña");
        assertThat(analysis.getAction()).contains("DB_PASSWORD", ".env");
    }

    @Test
    void missingDatabase() {
        FailureAnalysis analysis = analyze(wrapped(new SQLException("Unknown database 'jamix_fashion'", "42000")), Map.of());
        assertThat(analysis.getAction()).contains("CREATE DATABASE jamix_fashion");
    }

    @Test
    void otherFailuresAreLeftToSpringBoot() {
        assertThat(analyze(new IllegalStateException("something else"), Map.of())).isNull();
        assertThat(analyze(wrapped(new SQLException("Duplicate entry", "23000")), Map.of())).isNull();
    }

    @Test
    void readsHostAndPortFromTheUrl() {
        assertThat(DatabaseConnectionFailureAnalyzer.hostAndPort("jdbc:mysql://db.aiven.io:12345/jamix?x=1")).isEqualTo("db.aiven.io:12345");
        assertThat(DatabaseConnectionFailureAnalyzer.hostAndPort("jdbc:mysql://localhost/jamix")).isEqualTo("localhost:3306");
    }
}
