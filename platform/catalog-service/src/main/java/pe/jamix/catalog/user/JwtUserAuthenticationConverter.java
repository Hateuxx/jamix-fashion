package pe.jamix.catalog.user;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import pe.jamix.catalog.auth.TokenService;

/**
 * Turns a valid JWT into an authentication using the user's CURRENT state in the database, not the
 * roles frozen in the token. Deactivating someone, changing their role or resetting their password
 * therefore takes effect on their very next request.
 */
@Component
public class JwtUserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AppUserRepository users;

    public JwtUserAuthenticationConverter(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AppUser user = users.findByUsername(jwt.getSubject())
                .orElseThrow(() -> new DisabledException("Unknown or removed account"));
        if (!user.isActive()) {
            throw new DisabledException("Account deactivated");
        }
        Object sessionVersion = jwt.getClaim(TokenService.SESSION_VERSION_CLAIM);
        if (!(sessionVersion instanceof Number number) || number.intValue() != user.getSessionVersion()) {
            throw new CredentialsExpiredException("Session closed, please log in again");
        }
        return new JwtAuthenticationToken(jwt, AppUserDetailsService.authoritiesFor(user), user.getUsername());
    }
}
