package com.finder.demo.files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final Path documentationRoot;

    public FileController(@Value("${app.documentation-root:./documentation}") String root) {
        this.documentationRoot = Path.of(root).toAbsolutePath().normalize();
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "") String path) {
        Path directory = resolveInsideRoot(path);
        if (!Files.isDirectory(directory)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable");
        }
        try (Stream<Path> entries = Files.list(directory)) {
            List<Map<String, String>> items = entries.sorted(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER))
                    .map(item -> Map.of(
                            "name", item.getFileName().toString(),
                            "type", Files.isDirectory(item) ? "directory" : "file",
                            "extension", extension(item),
                            "path", documentationRoot.relativize(item).toString().replace('\\', '/'),
                            "openUri", item.toUri().toString()))
                    .toList();
            return Map.of("root", documentationRoot.toString(), "path", path, "items", items);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de lire le dossier", exception);
        }
    }

    private Path resolveInsideRoot(String path) {
        Path resolved = documentationRoot.resolve(path == null ? "" : path).normalize();
        if (!resolved.startsWith(documentationRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chemin non autorisé");
        }
        return resolved;
    }

    private String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1).toLowerCase() : "";
    }
}
