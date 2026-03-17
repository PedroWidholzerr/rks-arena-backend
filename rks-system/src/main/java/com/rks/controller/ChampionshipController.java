package com.rks.controller;

import com.rks.dto.ChampionshipDTO;
import com.rks.service.ChampionshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/championships")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ChampionshipController {
    @Autowired
    private ChampionshipService championshipService;

    @GetMapping
    public ResponseEntity<List<ChampionshipDTO>> getAllChampionships(
            @RequestParam(required = false) String seasonId) {
        if (seasonId != null) {
            return ResponseEntity.ok(championshipService.getChampionshipsBySeasonId(seasonId));
        }
        return ResponseEntity.ok(championshipService.getAllChampionships());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChampionshipDTO> getChampionshipById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(championshipService.getChampionshipById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChampionshipDTO> createChampionship(@RequestBody ChampionshipDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(championshipService.createChampionship(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChampionshipDTO> updateChampionship(@PathVariable String id, @RequestBody ChampionshipDTO dto) {
        try {
            return ResponseEntity.ok(championshipService.updateChampionship(id, dto));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteChampionship(@PathVariable String id) {
        try {
            championshipService.deleteChampionship(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/teams")
    public ResponseEntity<List<String>> getTeamsForChampionship(@PathVariable String id) {
        try {
            return ResponseEntity.ok(championshipService.getTeamIdsForChampionship(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/teams")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setTeamsForChampionship(
            @PathVariable String id,
            @RequestBody Map<String, List<String>> body) {
        try {
            List<String> teamIds = body.get("teamIds");
            championshipService.setTeamsForChampionship(id, teamIds != null ? teamIds : List.of());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
