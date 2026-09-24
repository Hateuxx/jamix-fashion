package pe.jamix.catalog.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class MissingDatabaseCredentialsFailureAnalyzerTest {

    @Test
    void onRenderItPointsToTheServiceEnvironmentNotToALocalEnvFile() {
        String action = MissingDatabaseCredentialsFailureAnalyzer.action(Map.of("RENDER", "true"));
        assertThat(action).contains("Estás en Render", "Environment", "DB_PASSWORD", "avnadmin")
                .doesNotContain("setup-mysql.sql", "npm run dev:h2");
    }

    @Test
    void onAPcItExplainsTheLocalSetup() {
        String action = MissingDatabaseCredentialsFailureAnalyzer.action(Map.of());
        assertThat(action).contains(".env.example", "setup-mysql.sql", "npm run dev:h2");
    }

    @Test
    void recognizesOtherPlatforms() {
        assertThat(MissingDatabaseCredentialsFailureAnalyzer.platform(Map.of("RAILWAY_ENVIRONMENT", "production"))).isEqualTo("Railway");
        assertThat(MissingDatabaseCredentialsFailureAnalyzer.platform(Map.of("FLY_APP_NAME", "jamix"))).isEqualTo("Fly.io");
    }
}
