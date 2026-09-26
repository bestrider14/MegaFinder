package com.finder.demo.testquotes;

import jakarta.validation.constraints.NotBlank;

public record TestTemplateRequest(
        @NotBlank String name,
        String description,
        String language,
        @NotBlank String content) {
}
