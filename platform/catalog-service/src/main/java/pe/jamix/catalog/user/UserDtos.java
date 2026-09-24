package pe.jamix.catalog.user;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {
    }

    /** Passwords: 8+ characters. Long passphrases are welcome; no composition rules (NIST 800-63B). */
    public static final int MIN_PASSWORD = 8;

    public record UserResponse(Long id, String username, String displayName, String role, String roleLabel,
            boolean active, boolean mustChangePassword, Instant createdAt) {

        static UserResponse from(AppUser u) {
            return new UserResponse(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole().name(),
                    u.getRole().label(), u.isActive(), u.isMustChangePassword(), u.getCreatedAt());
        }
    }

    public record CreateUserRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,40}", message = "3 a 40 letras, números, punto, guion o guion bajo")
            String username,
            @NotBlank @Size(max = 80) String displayName,
            @NotNull Role role,
            @NotBlank @Size(min = MIN_PASSWORD, max = 128) String temporaryPassword) {
    }

    /** Partial update: null fields are left as they are. */
    public record UpdateUserRequest(@Size(min = 1, max = 80) String displayName, Role role, Boolean active) {
    }

    public record ResetPasswordRequest(@NotBlank @Size(min = MIN_PASSWORD, max = 128) String temporaryPassword) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
            @NotBlank @Size(min = MIN_PASSWORD, max = 128) String newPassword) {
    }
}
