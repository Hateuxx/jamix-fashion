package pe.jamix.catalog.config;

/** Thrown at startup when the MySQL profile is active but DB_PASSWORD isn't set. */
public class MissingDatabaseCredentialsException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public MissingDatabaseCredentialsException() {
        super("DB_PASSWORD is not set");
    }
}
