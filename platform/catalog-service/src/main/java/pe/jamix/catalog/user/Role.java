package pe.jamix.catalog.user;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

/**
 * Staff roles are ordered: an OWNER can do everything a MANAGER can, a MANAGER everything a SELLER can.
 * CUSTOMER sits outside the staff ladder.
 */
public enum Role {
    CUSTOMER(0, "Cliente"),
    SELLER(1, "Vendedor(a)"),
    MANAGER(2, "Encargado(a)"),
    OWNER(3, "Dueño(a)");

    private final int level;
    private final String label;

    Role(int level, String label) {
        this.level = level;
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isStaff() {
        return this != CUSTOMER;
    }

    public boolean atLeast(Role other) {
        return isStaff() && level >= other.level;
    }

    public String authority() {
        return "ROLE_" + name();
    }

    /** Role of the current request, from its granted authorities (null if it has none, e.g. password change pending). */
    public static Role from(Collection<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority granted : authorities) {
            String authority = granted == null ? null : granted.getAuthority();
            if (authority != null && authority.startsWith("ROLE_")) {
                return valueOf(authority.substring("ROLE_".length()));
            }
        }
        return null;
    }
}
