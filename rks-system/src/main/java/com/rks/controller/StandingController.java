package com.rks.controller;

import com.rks.dto.StandingDTO;
import com.rks.service.StandingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/standings")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class StandingController {
    @Autowired
    private StandingService standingService;

    @GetMapping
    public ResponseEntity<List<StandingDTO>> getStandingsByChampionship(
            @RequestParam String championshipId) {
        try {
            return ResponseEntity.ok(standingService.getStandingsByChampionshipId(championshipId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<StandingDTO> getStandingById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(standingService.getStandingById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StandingDTO> createStanding(@RequestBody StandingDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(standingService.createStanding(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StandingDTO> updateStanding(@PathVariable String id, @RequestBody StandingDTO dto) {
        try {
            return ResponseEntity.ok(standingService.updateStanding(id, dto));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStanding(@PathVariable String id) {
        try {
            standingService.deleteStanding(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
