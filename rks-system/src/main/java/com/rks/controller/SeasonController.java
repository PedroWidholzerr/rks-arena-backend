package com.rks.controller;

import com.rks.dto.SeasonDTO;
import com.rks.service.SeasonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/seasons")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class SeasonController {
    @Autowired
    private SeasonService seasonService;

    @GetMapping
    public ResponseEntity<List<SeasonDTO>> getAllSeasons() {
        return ResponseEntity.ok(seasonService.getAllSeasons());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeasonDTO> getSeasonById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(seasonService.getSeasonById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeasonDTO> createSeason(@RequestBody SeasonDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(seasonService.createSeason(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeasonDTO> updateSeason(@PathVariable String id, @RequestBody SeasonDTO dto) {
        try {
            return ResponseEntity.ok(seasonService.updateSeason(id, dto));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSeason(@PathVariable String id) {
        try {
            seasonService.deleteSeason(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
