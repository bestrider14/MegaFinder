package com.finder.demo.products;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ProductRequest(
        Long projectId,
        @NotBlank String productNumber,
        @NotBlank String name,
        String description,
        String presentation,
        String assemblyElements,
        String assemblySteps,
        List<String> presentationImages,
        List<String> enclosureImages,
        String enclosurePinout,
        List<String> pcbImages,
        String pcbSpecifications,
        List<ProductContactRequest> contacts) {
}
