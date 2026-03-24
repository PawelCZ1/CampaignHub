package pl.pawelcz.campaignHub.product.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductRequest(
    @NotBlank(message = "Product name is mandatory")
    String name,
    String description
) {
}
