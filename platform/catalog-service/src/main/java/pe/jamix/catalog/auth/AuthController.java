package pe.jamix.catalog.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.jamix.catalog.user.AppUserDetailsService;
import pe.jamix.catalog.user.UserService;

/**
 * Exchanges credentials for a short-lived JWT. Bad credentials and deactivated accounts surface as a
 * 401 problem+json via ApiExceptionHandler. Put rate limiting in front of this endpoint at the gateway.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public record LoginRequest(@NotBlank @Size(max = 80) String username, @NotBlank @Size(max = 128) String password) {
    }

    private final AuthenticationManager authenticationManager;
    private final UserService users;

    public AuthController(AuthenticationManager authenticationManager, UserService users) {
        this.authenticationManager = authenticationManager;
        this.users = users;
    }

    @PostMapping("/login")
    public TokenService.Session login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                AppUserDetailsService.normalize(request.username()), request.password()));
        return users.sessionAfterLogin(authentication.getName());
    }
}
