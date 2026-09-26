package com.finder.demo.projects;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectRepository repository;

    public ProjectController(ProjectRepository repository) { this.repository = repository; }

    @GetMapping
    public List<Project> all() { return repository.findAll(); }

    @GetMapping("/{id}")
    public Project one(@PathVariable long id) { return repository.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Project create(@Valid @RequestBody ProjectRequest request) { return repository.create(request); }

    @PutMapping("/{id}")
    public Project update(@PathVariable long id, @Valid @RequestBody ProjectRequest request) {
        return repository.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { repository.delete(id); }
}
