package pe.jamix.catalog.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean mustChangePassword;

    /**
     * Copied into each JWT ("sv" claim). Bumping it logs the person out everywhere: their existing
     * tokens carry the old number. A counter, not a timestamp, because JWT times have 1 s precision.
     */
    @Column(nullable = false)
    private int sessionVersion;

    @Column(nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected AppUser() {
        // for JPA
    }

    public AppUser(String username, String displayName, String passwordHash, Role role, boolean mustChangePassword,
            Instant now) {
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
        this.createdAt = now;
    }

    /** The person chose a new password: pending flag cleared, other sessions closed. */
    public void changePassword(String newHash) {
        this.passwordHash = newHash;
        this.mustChangePassword = false;
        this.sessionVersion++;
    }

    /** The owner set a temporary password: the person must replace it at next login. */
    public void resetPassword(String temporaryHash) {
        this.passwordHash = temporaryHash;
        this.mustChangePassword = true;
        this.sessionVersion++;
    }

    public void changeRole(Role newRole) {
        if (newRole != role) {
            this.role = newRole;
            this.sessionVersion++;
        }
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            this.sessionVersion++;
        }
    }

    public void rename(String displayName) {
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public int getSessionVersion() {
        return sessionVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
