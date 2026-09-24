package pe.jamix.catalog.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Rendered as an RFC 9457 problem+json 409 by Spring MVC (duplicate slug, insufficient stock...). */
public class ConflictException extends ResponseStatusException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String detail) {
        super(HttpStatus.CONFLICT, detail);
    }
}
