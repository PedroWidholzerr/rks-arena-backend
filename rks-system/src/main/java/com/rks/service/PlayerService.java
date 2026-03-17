package com.rks.service;

import com.rks.dto.PlayerDTO;
import com.rks.entity.Player;
import com.rks.entity.Team;
import com.rks.repository.PlayerRepository;
import com.rks.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerService {
    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

    public List<PlayerDTO> getAllPlayers() {
        return playerRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PlayerDTO> getPlayersByTeamId(String teamId) {
        return playerRepository.findByTeamId(teamId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PlayerDTO getPlayerById(String id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        return convertToDTO(player);
    }

    public PlayerDTO createPlayer(PlayerDTO dto) {
        Team team = teamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new RuntimeException("Team not found"));

        Player player = Player.builder()
                .nickname(dto.getNickname())
                .tag(dto.getTag())
                .team(team)
                .isCaptain(dto.getIsCaptain() != null ? dto.getIsCaptain() : false)
                .notes(dto.getNotes())
                .build();
        player = playerRepository.save(player);
        return convertToDTO(player);
    }

    public PlayerDTO updatePlayer(String id, PlayerDTO dto) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        player.setNickname(dto.getNickname());
        player.setTag(dto.getTag());
        if (dto.getIsCaptain() != null) {
            player.setIsCaptain(dto.getIsCaptain());
        }
        if (dto.getNotes() != null) {
            player.setNotes(dto.getNotes());
        }
        player = playerRepository.save(player);
        return convertToDTO(player);
    }

    public void deletePlayer(String id) {
        playerRepository.deleteById(id);
    }

    private PlayerDTO convertToDTO(Player player) {
        return PlayerDTO.builder()
                .id(player.getId())
                .nickname(player.getNickname())
                .tag(player.getTag())
                .teamId(player.getTeam().getId())
                .isCaptain(player.getIsCaptain())
                .notes(player.getNotes())
                .build();
    }
}
