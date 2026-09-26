package com.finder.demo.projects;

import java.time.LocalDateTime;

public record Project(Long id, String projectNumber, String name, String contactPerson,
                      String description, String status, String documentationPath, LocalDateTime createdAt,
                      LocalDateTime updatedAt) {
}
