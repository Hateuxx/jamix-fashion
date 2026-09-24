package pe.jamix.catalog.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*") String slug,
        @NotBlank @Size(max = 120) String name,
        @NotNull Category category,
        @Size(max = 60) String type,
        /* Optional: UNISEX when omitted. */
        Gender gender,
        @Size(max = 500) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @DecimalMin("0.00") BigDecimal priceMax,
        @DecimalMin("0.00") BigDecimal wholesalePrice,
        @DecimalMin("0.00") BigDecimal wholesalePriceMax,
        @Size(max = 255) String imageUrl,
        @PositiveOrZero int stock,
        /* Version the client last saw; required on update so concurrent orders aren't overwritten. */
        Long version) {

    @AssertTrue(message = "priceMax must be greater than or equal to price")
    public boolean isPriceRangeValid() {
        return price == null || priceMax == null || priceMax.compareTo(price) >= 0;
    }

    @AssertTrue(message = "wholesalePrice must not exceed price")
    public boolean isWholesalePriceValid() {
        return price == null || wholesalePrice == null || wholesalePrice.compareTo(price) <= 0;
    }

    @AssertTrue(message = "wholesalePriceMax requires wholesalePrice and must be greater than or equal to it")
    public boolean isWholesaleRangeValid() {
        if (wholesalePriceMax == null) {
            return true;
        }
        return wholesalePrice != null && wholesalePriceMax.compareTo(wholesalePrice) >= 0;
    }
}
