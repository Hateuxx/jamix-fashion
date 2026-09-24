package pe.jamix.catalog.stock;

import pe.jamix.catalog.user.Role;

/**
 * Why a product's stock changed. {@code sign} is the direction; {@code minRole} is the lowest staff
 * role that may register it by hand (null = only the system records it).
 */
public enum MovementType {
    INGRESO(+1, "Ingreso de mercadería", Role.MANAGER),
    VENTA_TIENDA(-1, "Venta en tienda", Role.SELLER),
    VENTA_WHATSAPP(-1, "Venta por WhatsApp", Role.SELLER),
    DEVOLUCION(+1, "Devolución", Role.MANAGER),
    MERMA(-1, "Merma o prenda dañada", Role.MANAGER),
    /** Physical count: sets the stock to what's on the shelf (PATCH .../stock). */
    AJUSTE(0, "Ajuste por conteo", null),
    /** An order placed on the website. */
    VENTA_WEB(-1, "Pedido web", null);

    private final int sign;
    private final String label;
    private final Role minRole;

    MovementType(int sign, String label, Role minRole) {
        this.sign = sign;
        this.label = label;
        this.minRole = minRole;
    }

    public int sign() {
        return sign;
    }

    public String label() {
        return label;
    }

    /** Registered by hand through POST .../movements (AJUSTE and VENTA_WEB have their own flows). */
    public boolean isManual() {
        return minRole != null;
    }

    public boolean allowedFor(Role role) {
        return isManual() && role != null && role.atLeast(minRole);
    }
}
