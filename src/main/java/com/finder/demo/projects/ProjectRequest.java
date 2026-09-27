package com.finder.demo.projects;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ProjectRequest(
        @NotBlank(message = "Le numéro du projet est obligatoire") String projectNumber,
        @NotBlank(message = "Le nom du projet est obligatoire") String name,
        String contactPerson,
        String description,
        String status,
        String presentation,
        String assemblyElements,
        String assemblySteps,
        List<String> presentationImages,
        List<String> enclosureImages,
        String enclosurePinout,
        List<String> pcbImages,
        String pcbSpecifications,
        List<ProjectContactRequest> contacts) {
}
