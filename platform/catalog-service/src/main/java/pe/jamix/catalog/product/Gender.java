package pe.jamix.catalog.product;

/**
 * Who a garment is cut for. Decides which family members the store offers in "¿Para quién es?"
 * (a dress can't go in Papá's look). Separate from Category because "Niños" mixes girls' dresses
 * with boys' sets.
 */
public enum Gender {
    FEMENINO,
    MASCULINO,
    UNISEX;

    public String slug() {
        return name().toLowerCase();
    }
}
