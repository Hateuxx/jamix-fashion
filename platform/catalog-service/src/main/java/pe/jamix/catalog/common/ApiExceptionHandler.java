package pe.jamix.catalog.common;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the few exceptions Spring doesn't already turn into problem+json.
 * Validation errors and {@code ResponseStatusException}s are handled by
 * {@code spring.mvc.problemdetails.enabled=true}.
 */
@RestControllerAdvice(basePackages = "pe.jamix.catalog")
public class ApiExceptionHandler {

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concurrentModification(OptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "El producto fue modificado por otra operación. Vuelve a intentarlo.");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail badCredentials(AuthenticationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.");
    }
}
