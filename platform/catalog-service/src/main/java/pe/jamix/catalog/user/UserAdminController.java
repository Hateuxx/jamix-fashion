package pe.jamix.catalog.user;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.jamix.catalog.user.UserDtos.CreateUserRequest;
import pe.jamix.catalog.user.UserDtos.ResetPasswordRequest;
import pe.jamix.catalog.user.UserDtos.UpdateUserRequest;
import pe.jamix.catalog.user.UserDtos.UserResponse;

/** Team accounts. OWNER only (see SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {

    private final UserService users;

    public UserAdminController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserResponse> team() {
        return users.team();
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = users.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/users/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable long id, @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        return users.update(id, request, authentication.getName());
    }

    @PostMapping("/{id}/reset-password")
    public UserResponse resetPassword(@PathVariable long id, @Valid @RequestBody ResetPasswordRequest request) {
        return users.resetPassword(id, request.temporaryPassword());
    }
}
