package pe.jamix.catalog.config;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Turns a missing database password into Spring Boot's short "APPLICATION FAILED TO START"
 * report (Description / Action) instead of a stack trace: it's a setup step, not a bug.
 */
public class MissingDatabaseCredentialsFailureAnalyzer
        extends AbstractFailureAnalyzer<MissingDatabaseCredentialsException> {

    @Override
    protected FailureAnalysis analyze(@NonNull Throwable rootFailure, @NonNull MissingDatabaseCredentialsException cause) {
        return new FailureAnalysis(
                "Falta la contraseña de la base de datos: no hay DB_PASSWORD en catalog-service/.env"
                        + " ni en las variables de entorno.",
                """
                Configura MySQL una sola vez (detalles en el README.md de la raíz):
                  1. Crea la base y los usuarios con db/setup-mysql.sql (copia local .local.sql con tus contraseñas).
                  2. Copia catalog-service/.env.example como catalog-service/.env
                  3. Completa DB_PASSWORD, DB_MIGRATION_PASSWORD, JAMIX_JWT_SECRET y las contraseñas de Jamix.

                ¿Solo quieres probar la tienda sin MySQL? Usa:  npm run dev:h2""",
                cause);
    }
}
