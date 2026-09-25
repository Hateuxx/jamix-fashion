package pe.jamix.catalog.config;

import java.sql.SQLException;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.core.env.Environment;

/**
 * Turns the three usual MySQL start-up failures into a short "APPLICATION FAILED TO START" report
 * instead of a long stack trace ending in "Communications link failure":
 * the server isn't reachable, the credentials are wrong, or the database doesn't exist.
 * Advice differs on a PC (start the MySQL service) and on a hosting platform (check the variables).
 */
public class DatabaseConnectionFailureAnalyzer implements FailureAnalyzer {

    enum Problem { UNREACHABLE, ACCESS_DENIED, UNKNOWN_DATABASE }

    private final Environment environment;
    private final Map<String, String> systemEnv;

    /** Spring Boot creates failure analyzers passing the Environment. */
    public DatabaseConnectionFailureAnalyzer(Environment environment) {
        this(environment, System.getenv());
    }

    DatabaseConnectionFailureAnalyzer(Environment environment, Map<String, String> systemEnv) {
        this.environment = environment;
        this.systemEnv = systemEnv;
    }

    @Override
    public @Nullable FailureAnalysis analyze(@NonNull Throwable failure) {
        Problem problem = classify(failure);
        if (problem == null) {
            return null; // not ours: let Spring Boot report it normally
        }
        String url = environment.getProperty("spring.datasource.url", "");
        String target = hostAndPort(url);
        String platform = MissingDatabaseCredentialsFailureAnalyzer.platform(systemEnv);
        return switch (problem) {
            case UNREACHABLE -> new FailureAnalysis(
                    "No se pudo conectar a MySQL en " + target + ": el servidor no responde.",
                    platform == null ? """
                            MySQL no está en marcha en tu PC (su servicio de Windows suele tener inicio manual).
                              - Inícialo: abre PowerShell COMO ADMINISTRADOR y ejecuta:  net start <servicio>
                                (el nombre aparece en services.msc, p. ej. MySQL80). Para que arranque siempre:
                                services.msc → tu servicio de MySQL → Tipo de inicio: Automático.
                              - O prueba sin MySQL:  npm run dev:h2"""
                            : """
                            En %s: revisa que el HOST y PORT de DB_URL sean los de tu base (Aiven → Overview →
                            Connection information) y que el servicio de la base esté en estado Running.""".formatted(platform),
                    failure);
            case ACCESS_DENIED -> new FailureAnalysis(
                    "MySQL rechazó el usuario o la contraseña (" + target + ").",
                    "Revisa que DB_USERNAME y DB_PASSWORD (y DB_MIGRATION_* si las usas) coincidan con las de la base"
                            + (platform == null ? " (archivo catalog-service/.env)." : " (" + platform + " → Environment)."),
                    failure);
            case UNKNOWN_DATABASE -> new FailureAnalysis(
                    "La base de datos indicada en DB_URL no existe en " + target + ".",
                    "Créala una vez (las tablas las crea Flyway al arrancar):  CREATE DATABASE jamix_fashion "
                            + "CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;  (en Aiven: pestaña Databases → Add database)",
                    failure);
        };
    }

    /** Looks through the cause chain for the MySQL error behind the failure. */
    static @Nullable Problem classify(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLException sql) {
                String state = sql.getSQLState() == null ? "" : sql.getSQLState();
                String message = sql.getMessage() == null ? "" : sql.getMessage();
                if (state.startsWith("08") || message.contains("Communications link failure")) {
                    return Problem.UNREACHABLE;
                }
                if ("28000".equals(state) || message.startsWith("Access denied")) {
                    return Problem.ACCESS_DENIED;
                }
                if (message.startsWith("Unknown database")) {
                    return Problem.UNKNOWN_DATABASE;
                }
            }
        }
        return null;
    }

    /** "jdbc:mysql://host:3306/db?x" -> "host:3306" (defaults to 3306 when the URL has no port). */
    static String hostAndPort(String url) {
        int start = url.indexOf("//");
        if (start < 0) {
            return "(DB_URL sin host)";
        }
        String rest = url.substring(start + 2);
        int end = rest.indexOf('/');
        String hostPort = end < 0 ? rest : rest.substring(0, end);
        return hostPort.contains(":") ? hostPort : hostPort + ":3306";
    }
}
