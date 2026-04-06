package com.rks.service;

import com.rks.dto.TeamDTO;
import com.rks.entity.Team;
import com.rks.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamService {
    @Autowired
    private TeamRepository teamRepository;

    public List<TeamDTO> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public TeamDTO getTeamById(String id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        return convertToDTO(team);
    }

    public TeamDTO createTeam(TeamDTO dto) {
        if (teamRepository.findByTag(dto.getTag()).isPresent()) {
            throw new RuntimeException("Team tag already exists");
        }

        Team team = Team.builder()
                .name(dto.getName())
                .tag(dto.getTag())
                .description(dto.getDescription())
                .build();
        team = teamRepository.save(team);
        return convertToDTO(team);
    }

    public TeamDTO updateTeam(String id, TeamDTO dto) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        team.setName(dto.getName());
        team.setTag(dto.getTag());
        team.setDescription(dto.getDescription());
        if (dto.getLogoUrl() != null) {
            team.setLogoUrl(dto.getLogoUrl());
        }
        team = teamRepository.save(team);
        return convertToDTO(team);
    }

    public TeamDTO updateTeamLogo(String id, String logoUrl, byte[] logoData, String contentType) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        team.setLogoUrl(logoUrl);
        team.setLogoData(logoData);
        team.setLogoContentType(contentType);
        team = teamRepository.save(team);
        return convertToDTO(team);
    }

    public void deleteTeam(String id) {
        teamRepository.deleteById(id);
    }

    public Team getTeamEntity(String id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found"));
    }

    private TeamDTO convertToDTO(Team team) {
        return TeamDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .tag(team.getTag())
                .description(team.getDescription())
                .logoUrl(team.getLogoUrl())
                .build();
    }
}
