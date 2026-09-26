package com.finder.demo.projects;

import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(
        @NotBlank(message = "Le numéro du projet est obligatoire") String projectNumber,
        @NotBlank(message = "Le nom du projet est obligatoire") String name,
        String contactPerson,
        String description,
        String status) {
}
