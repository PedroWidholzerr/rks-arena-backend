package com.rks.service;

import com.rks.dto.ChampionshipDTO;
import com.rks.entity.Championship;
import com.rks.entity.Season;
import com.rks.entity.Standing;
import com.rks.entity.Team;
import com.rks.repository.ChampionshipRepository;
import com.rks.repository.SeasonRepository;
import com.rks.repository.StandingRepository;
import com.rks.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChampionshipService {
    @Autowired
    private ChampionshipRepository championshipRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private StandingRepository standingRepository;

    @Autowired
    private TeamRepository teamRepository;

    public List<ChampionshipDTO> getAllChampionships() {
        return championshipRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ChampionshipDTO> getChampionshipsBySeasonId(String seasonId) {
        return championshipRepository.findBySeasonId(seasonId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ChampionshipDTO getChampionshipById(String id) {
        Championship championship = championshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Championship not found"));
        return convertToDTO(championship);
    }

    public ChampionshipDTO createChampionship(ChampionshipDTO dto) {
        Season season = seasonRepository.findById(dto.getSeasonId())
                .orElseThrow(() -> new RuntimeException("Season not found"));

        Championship championship = Championship.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .season(season)
                .status(dto.getStatus() != null
                        ? Championship.ChampionshipStatus.valueOf(dto.getStatus().toUpperCase())
                        : Championship.ChampionshipStatus.UPCOMING)
                .build();
        championship = championshipRepository.save(championship);
        return convertToDTO(championship);
    }

    public ChampionshipDTO updateChampionship(String id, ChampionshipDTO dto) {
        Championship championship = championshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Championship not found"));
        championship.setName(dto.getName());
        championship.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            championship.setStatus(Championship.ChampionshipStatus.valueOf(dto.getStatus().toUpperCase()));
        }
        championship = championshipRepository.save(championship);
        return convertToDTO(championship);
    }

    public void deleteChampionship(String id) {
        championshipRepository.deleteById(id);
    }

    public List<String> getTeamIdsForChampionship(String id) {
        return standingRepository.findByChampionshipId(id).stream()
                .map(s -> s.getTeam().getId())
                .collect(Collectors.toList());
    }

    public void setTeamsForChampionship(String id, List<String> teamIds) {
        Championship championship = championshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Championship not found"));

        List<Standing> existing = standingRepository.findByChampionshipId(id);
        Set<String> existingTeamIds = existing.stream()
                .map(s -> s.getTeam().getId())
                .collect(Collectors.toSet());
        Set<String> newTeamIds = new HashSet<>(teamIds);

        // Add new teams
        for (String teamId : newTeamIds) {
            if (!existingTeamIds.contains(teamId)) {
                Team team = teamRepository.findById(teamId)
                        .orElseThrow(() -> new RuntimeException("Team not found: " + teamId));
                Standing standing = Standing.builder()
                        .championship(championship)
                        .team(team)
                        .wins(0)
                        .losses(0)
                        .points(0)
                        .build();
                standingRepository.save(standing);
            }
        }

        // Remove teams not in the new list
        for (Standing standing : existing) {
            if (!newTeamIds.contains(standing.getTeam().getId())) {
                standingRepository.delete(standing);
            }
        }
    }

    private ChampionshipDTO convertToDTO(Championship championship) {
        return ChampionshipDTO.builder()
                .id(championship.getId())
                .name(championship.getName())
                .description(championship.getDescription())
                .seasonId(championship.getSeason().getId())
                .status(championship.getStatus().name().toLowerCase())
                .build();
    }
}
