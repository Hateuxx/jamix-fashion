package pe.jamix.catalog.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import pe.jamix.catalog.user.JwtUserAuthenticationConverter;

/**
 * Stateless JWT security.
 *
 * <ul>
 *   <li>Public: SSR storefront ("/"), static assets, GET catalog, login.</li>
 *   <li>CUSTOMER or ADMIN: order creation and history.</li>
 *   <li>ADMIN: everything under /api/v1/admin (stock management).</li>
 * </ul>
 *
 * Tokens are HS256-signed with a shared secret, which is enough while this service is the only
 * issuer. When a dedicated identity provider (Keycloak, Auth0, Cognito...) is introduced, replace
 * the encoder/decoder with {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String DEV_SECRET_MARKER = "dev-only";

    private final JamixProperties properties;

    public SecurityConfig(JamixProperties properties) {
        this.properties = properties;
    }

    private static final String[] STAFF = { "OWNER", "MANAGER", "SELLER" };

    /**
     * Permissions by role:
     * <ul>
     *   <li>SELLER: sees stock and history, registers sales (which movement types: StockService).</li>
     *   <li>MANAGER: also deliveries, returns, write-offs, physical counts and product edits.</li>
     *   <li>OWNER: also the team (accounts and roles).</li>
     *   <li>Someone with a temporary password only reaches /api/v1/me until they replace it.</li>
     * </ul>
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtUserAuthenticationConverter jwtConverter)
            throws Exception {
        http
                // No cookies or server sessions: every API call carries a bearer token, so CSRF doesn't apply.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(contentSecurityPolicy())))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/", "/css/**", "/img/**", "/favicon.ico", "/favicon.svg",
                                "/robots.txt")
                        .permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/me", "/api/v1/me/**").authenticated()
                        .requestMatchers("/api/v1/admin/users", "/api/v1/admin/users/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/**").hasAnyRole(STAFF)
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/products/*/movements").hasAnyRole(STAFF)
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("OWNER", "MANAGER")
                        .requestMatchers("/api/v1/orders", "/api/v1/orders/**").hasAnyRole("CUSTOMER", "OWNER", "MANAGER")
                        .anyRequest().authenticated())
                // Roles come from the database on every request, not from the token (see the converter).
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));
        return http.build();
    }

    /** The SSR page loads only its own CSS/images plus Google Fonts, and posts the lead form to the Node BFF. */
    private String contentSecurityPolicy() {
        String leadsOrigin = origin(properties.storefront().leadsEndpoint());
        return String.join("; ",
                "default-src 'self'",
                "img-src 'self' data:",
                "style-src 'self' https://fonts.googleapis.com",
                "font-src 'self' https://fonts.gstatic.com",
                "script-src 'self'",
                // The BFF answers the form with a redirect to WhatsApp; browsers check form-action on redirects too.
                "form-action 'self' " + leadsOrigin + " https://wa.me https://api.whatsapp.com https://web.whatsapp.com",
                "frame-ancestors 'none'",
                "base-uri 'self'",
                "object-src 'none'");
    }

    private static String origin(String url) {
        URI uri = URI.create(url);
        return uri.getScheme() + "://" + uri.getAuthority();
    }

    @Bean
    SecretKey jwtSigningKey() {
        String secret = properties.security().jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jamix.security.jwt.secret must be at least 32 bytes for HS256");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secret.startsWith(DEV_SECRET_MARKER)) {
            log.warn("Using the development JWT secret. Set JAMIX_JWT_SECRET before deploying.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.security().jwt().issuer()));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // Understands {bcrypt}, {argon2}... prefixes, so production can supply pre-hashed passwords.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Accounts live in app_users (AppUserDetailsService); the first ones are created by UserBootstrap. */
    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
