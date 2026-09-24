package pe.jamix.catalog.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Rendered as an RFC 9457 problem+json 404 by Spring MVC. */
public class NotFoundException extends ResponseStatusException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, detail);
    }
}
