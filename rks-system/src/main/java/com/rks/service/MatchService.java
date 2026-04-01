package com.rks.service;

import com.rks.dto.MatchDTO;
import com.rks.entity.Championship;
import com.rks.entity.Match;
import com.rks.entity.Player;
import com.rks.entity.Team;
import com.rks.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchService {
    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ChampionshipRepository championshipRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private StandingService standingService;

    public List<MatchDTO> getAllMatches() {
        return matchRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<MatchDTO> getMatchesByChampionshipId(String championshipId) {
        return matchRepository.findByChampionshipId(championshipId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<MatchDTO> getMatchesByTeamId(String teamId) {
        return matchRepository.findByTeamAIdOrTeamBId(teamId, teamId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public MatchDTO getMatchById(String id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        return convertToDTO(match);
    }

    public MatchDTO createMatch(MatchDTO dto) {
        Championship championship = championshipRepository.findById(dto.getChampionshipId())
                .orElseThrow(() -> new RuntimeException("Championship not found"));
        Team teamA = teamRepository.findById(dto.getTeamAId())
                .orElseThrow(() -> new RuntimeException("Team A not found"));
        Team teamB = teamRepository.findById(dto.getTeamBId())
                .orElseThrow(() -> new RuntimeException("Team B not found"));

        Match match = Match.builder()
                .championship(championship)
                .teamA(teamA)
                .teamB(teamB)
                .scheduledAt(dto.getScheduledAt())
                .scoreA(dto.getScoreA() != null ? dto.getScoreA() : 0)
                .scoreB(dto.getScoreB() != null ? dto.getScoreB() : 0)
                .build();
        match = matchRepository.save(match);
        standingService.ensureStandingsExist(championship.getId(), teamA.getId(), teamB.getId());

        // Update standings if match already has scores
        int sA = match.getScoreA() != null ? match.getScoreA() : 0;
        int sB = match.getScoreB() != null ? match.getScoreB() : 0;
        if (sA != 0 || sB != 0) {
            standingService.updateStandingsForMatch(match);
        }

        return convertToDTO(match);
    }

    public MatchDTO updateMatch(String id, MatchDTO dto) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        // Reset standings for old scores before updating
        standingService.resetStandingsForMatch(match);
        
        if (dto.getScheduledAt() != null) match.setScheduledAt(dto.getScheduledAt());
        if (dto.getScoreA() != null) match.setScoreA(dto.getScoreA());
        if (dto.getScoreB() != null) match.setScoreB(dto.getScoreB());
        if (dto.getComment() != null) match.setComment(dto.getComment());
        
        if (dto.getMvpPlayerId() != null && !dto.getMvpPlayerId().isEmpty()) {
            Player mvpPlayer = playerRepository.findById(dto.getMvpPlayerId()).orElse(null);
            match.setMvpPlayer(mvpPlayer);
        } else {
            match.setMvpPlayer(null);
        }
        
        match = matchRepository.save(match);
        
        // Update standings
        standingService.updateStandingsForMatch(match);
        
        return convertToDTO(match);
    }

    public void deleteMatch(String id) {
        Match match = matchRepository.findById(id).orElseThrow();
        standingService.resetStandingsForMatch(match);
        matchRepository.deleteById(id);
    }

    private MatchDTO convertToDTO(Match match) {
        return MatchDTO.builder()
                .id(match.getId())
                .championshipId(match.getChampionship().getId())
                .teamAId(match.getTeamA().getId())
                .teamBId(match.getTeamB().getId())
                .scheduledAt(match.getScheduledAt())
                .scoreA(match.getScoreA())
                .scoreB(match.getScoreB())
                .comment(match.getComment())
                .mvpPlayerId(match.getMvpPlayer() != null ? match.getMvpPlayer().getId() : null)
                .build();
    }
}
