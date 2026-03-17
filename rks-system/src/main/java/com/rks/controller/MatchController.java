package com.rks.controller;

import com.rks.dto.MatchDTO;
import com.rks.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class MatchController {
    @Autowired
    private MatchService matchService;

    @GetMapping
    public ResponseEntity<List<MatchDTO>> getAllMatches(
            @RequestParam(required = false) String championshipId,
            @RequestParam(required = false) String teamId) {
        if (championshipId != null) {
            return ResponseEntity.ok(matchService.getMatchesByChampionshipId(championshipId));
        }
        if (teamId != null) {
            return ResponseEntity.ok(matchService.getMatchesByTeamId(teamId));
        }
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDTO> getMatchById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(matchService.getMatchById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatchDTO> createMatch(@RequestBody MatchDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(matchService.createMatch(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatchDTO> updateMatch(@PathVariable String id, @RequestBody MatchDTO dto) {
        try {
            return ResponseEntity.ok(matchService.updateMatch(id, dto));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMatch(@PathVariable String id) {
        try {
            matchService.deleteMatch(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
