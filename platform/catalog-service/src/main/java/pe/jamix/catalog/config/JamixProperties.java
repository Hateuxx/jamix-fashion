package pe.jamix.catalog.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed view of the {@code jamix.*} block in application.yml.
 */
@ConfigurationProperties(prefix = "jamix")
public record JamixProperties(Security security, Cors cors, Storefront storefront) {

    public record Security(Jwt jwt, List<User> users) {
    }

    public record Jwt(String secret, String issuer, Duration ttl) {
    }

    /** A bootstrap account. {@code password} may be plain (dev) or pre-hashed, e.g. {@code {bcrypt}$2a$...}. */
    public record User(String username, String password, List<String> roles) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    /** {@code shopUrl}: the interactive React store (bag, filters, quick view). */
    public record Storefront(String whatsappNumber, String leadsEndpoint, String siteUrl, String shopUrl) {
    }
}
