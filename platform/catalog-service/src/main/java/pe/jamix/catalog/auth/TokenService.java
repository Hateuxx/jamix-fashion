package pe.jamix.catalog.auth;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import pe.jamix.catalog.config.JamixProperties;
import pe.jamix.catalog.user.AppUser;

@Service
public class TokenService {

    /** What a client gets after logging in or changing its password. */
    public record Session(String accessToken, String tokenType, long expiresIn, String username, String displayName,
            String role, String roleLabel, List<String> roles, boolean mustChangePassword) {
    }

    /** Must match AppUser.sessionVersion for the token to be accepted (see JwtUserAuthenticationConverter). */
    public static final String SESSION_VERSION_CLAIM = "sv";

    private final JwtEncoder encoder;
    private final JamixProperties.Jwt settings;

    public TokenService(JwtEncoder encoder, JamixProperties properties) {
        this.encoder = encoder;
        this.settings = properties.security().jwt();
    }

    /**
     * The roles claim is informative (for the UI). Permissions are always re-read from the database on
     * each request by JwtUserAuthenticationConverter.
     */
    public Session issue(AppUser user) {
        Instant now = Instant.now();
        List<String> roles = user.isMustChangePassword() ? List.of() : List.of(user.getRole().name());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(settings.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(settings.ttl()))
                .subject(user.getUsername())
                .claim("roles", roles)
                .claim("name", user.getDisplayName())
                .claim(SESSION_VERSION_CLAIM, user.getSessionVersion())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Session(token, "Bearer", settings.ttl().toSeconds(), user.getUsername(), user.getDisplayName(),
                user.getRole().name(), user.getRole().label(), roles, user.isMustChangePassword());
    }
}
