package com.finder.demo.testquotes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TestQuoteRequest(
        @NotNull Long projectId,
        Long benchId,
        Long templateId,
        @NotBlank String name,
        String description,
        String status,
        List<TestQuoteReferenceRequest> references,
        List<TestStepRequest> steps) {
}
