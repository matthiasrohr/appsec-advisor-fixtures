package com.example.threatfixture.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import com.example.threatfixture.config.AppProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final AppProperties appProperties;

    public FileController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam MultipartFile file,
            @RequestParam(defaultValue = "public") String folder) throws IOException {
        Path uploadRoot = Paths.get(appProperties.getUploadRoot());
        Path destination = uploadRoot.resolve(folder).resolve(file.getOriginalFilename());
        Files.createDirectories(destination.getParent());
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return Map.of("storedPath", destination.toString(), "size", file.getSize());
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String path) {
        Path uploadRoot = Paths.get(appProperties.getUploadRoot());
        Path requestedPath = uploadRoot.resolve(path);
        Resource resource = new FileSystemResource(requestedPath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + requestedPath.getFileName() + "\"")
                .body(resource);
    }
}

