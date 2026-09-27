package com.finder.demo.projects;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record Project(Long id, String projectNumber, String name, String contactPerson,
                      String description, String status, String documentationPath, LocalDateTime createdAt,
                      LocalDateTime updatedAt, String presentation, String assemblyElements,
                      String assemblySteps, List<String> presentationImages, List<String> enclosureImages,
                      String enclosurePinout, List<String> pcbImages, String pcbSpecifications,
                      List<Map<String, Object>> contacts, List<Map<String, Object>> quotes) {
}
