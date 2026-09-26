package com.finder.demo.projects;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectActivityController {
    private final ProjectRepository repository;

    public ProjectActivityController(ProjectRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}/activity")
    public List<Map<String, Object>> activity(@PathVariable long id) {
        return repository.findActivity(id);
    }
}
