package com.rks.controller;

import com.rks.dto.PlayerDTO;
import com.rks.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class PlayerController {
    @Autowired
    private PlayerService playerService;

    @GetMapping
    public ResponseEntity<List<PlayerDTO>> getAllPlayers(
            @RequestParam(required = false) String teamId) {
        if (teamId != null) {
            return ResponseEntity.ok(playerService.getPlayersByTeamId(teamId));
        }
        return ResponseEntity.ok(playerService.getAllPlayers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerDTO> getPlayerById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(playerService.getPlayerById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlayerDTO> createPlayer(@RequestBody PlayerDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(playerService.createPlayer(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlayerDTO> updatePlayer(@PathVariable String id, @RequestBody PlayerDTO dto) {
        try {
            return ResponseEntity.ok(playerService.updatePlayer(id, dto));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePlayer(@PathVariable String id) {
        try {
            playerService.deletePlayer(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
