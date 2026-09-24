package pe.jamix.catalog.config;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Turns a missing database password into Spring Boot's short "APPLICATION FAILED TO START"
 * report (Description / Action) instead of a stack trace: it's a setup step, not a bug.
 * The advice depends on where the app runs: a local .env on a PC, the service's environment
 * variables on a hosting platform.
 */
public class MissingDatabaseCredentialsFailureAnalyzer
        extends AbstractFailureAnalyzer<MissingDatabaseCredentialsException> {

    @Override
    protected FailureAnalysis analyze(@NonNull Throwable rootFailure, MissingDatabaseCredentialsException cause) {
        return new FailureAnalysis(
                "Falta la contraseña de la base de datos: la variable DB_PASSWORD no está definida o está vacía.",
                action(System.getenv()),
                cause);
    }

    /** Hosting platforms set these variables in every container they run. */
    static String platform(Map<String, String> env) {
        if (env.containsKey("RENDER")) {
            return "Render";
        }
        if (env.containsKey("RAILWAY_ENVIRONMENT")) {
            return "Railway";
        }
        if (env.containsKey("FLY_APP_NAME")) {
            return "Fly.io";
        }
        return null;
    }

    static String action(Map<String, String> env) {
        String platform = platform(env);
        if (platform != null) {
            return """
                    Estás en %s: el archivo .env no existe en el servidor, las credenciales van en las variables
                    de entorno del servicio (%s → jamix-catalog → Environment):
                      DB_URL       jdbc:mysql://HOST:PORT/jamix_fashion?sslMode=REQUIRED&connectionTimeZone=UTC&characterEncoding=UTF-8
                      DB_USERNAME  el usuario de tu MySQL (en Aiven: avnadmin)
                      DB_PASSWORD  su contraseña
                    Guarda los cambios y el servicio se vuelve a desplegar solo. Guía completa: DEPLOY.md.""".formatted(
                    platform, platform);
        }
        return """
                Configura MySQL una sola vez (detalles en el README.md de la raíz):
                  1. Crea la base y los usuarios con db/setup-mysql.sql (copia local .local.sql con tus contraseñas).
                  2. Copia catalog-service/.env.example como catalog-service/.env
                  3. Completa DB_PASSWORD, DB_MIGRATION_PASSWORD, JAMIX_JWT_SECRET y las contraseñas de Jamix.

                ¿Solo quieres probar la tienda sin MySQL? Usa:  npm run dev:h2""";
    }
}
