package com.rks.controller;

import com.rks.dto.TeamDTO;
import com.rks.service.TeamService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class TeamController {
    @Autowired
    private TeamService teamService;

    private final Path logoUploadDir;

    public TeamController(@Value("${app.upload.team-logos-dir:uploads/team-logos}") String teamLogosDir) {
        this.logoUploadDir = Paths.get(teamLogosDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.logoUploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize team logo directory", e);
        }
    }

    @GetMapping
    public ResponseEntity<List<TeamDTO>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamDTO> getTeamById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(teamService.getTeamById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamDTO> createTeam(@RequestBody TeamDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamDTO> updateTeam(@PathVariable String id, @RequestBody TeamDTO dto) {
        try {
            return ResponseEntity.ok(teamService.updateTeam(id, dto));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/logo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamDTO> uploadLogo(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "logo";
            String extension = "";
            int extensionIndex = originalFilename.lastIndexOf('.');
            if (extensionIndex >= 0) {
                extension = originalFilename.substring(extensionIndex);
            }

            String storedFilename = id + "-" + UUID.randomUUID() + extension;
            Path targetPath = logoUploadDir.resolve(storedFilename).normalize();
            if (!targetPath.startsWith(logoUploadDir)) {
                return ResponseEntity.badRequest().build();
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String logoUrl = "/api/teams/logos/" + storedFilename;
            return ResponseEntity.ok(teamService.updateTeamLogo(id, logoUrl));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/logos/{filename:.+}")
    public ResponseEntity<Resource> getLogo(@PathVariable String filename) {
        try {
            Path filePath = logoUploadDir.resolve(filename).normalize();
            if (!filePath.startsWith(logoUploadDir)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            MediaType mediaType = contentType != null
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTeam(@PathVariable String id) {
        try {
            teamService.deleteTeam(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
