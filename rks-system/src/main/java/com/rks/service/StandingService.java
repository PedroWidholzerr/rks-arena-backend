package com.rks.service;

import com.rks.dto.StandingDTO;
import com.rks.entity.Championship;
import com.rks.entity.Match;
import com.rks.entity.Standing;
import com.rks.entity.Team;
import com.rks.repository.ChampionshipRepository;
import com.rks.repository.MatchRepository;
import com.rks.repository.StandingRepository;
import com.rks.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StandingService {
    @Autowired
    private StandingRepository standingRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private ChampionshipRepository championshipRepository;

    @Autowired
    private MatchRepository matchRepository;

    public List<StandingDTO> getStandingsByChampionshipId(String championshipId) {
        return standingRepository.findByChampionshipId(championshipId).stream()
                .sorted((a, b) -> b.getPoints().compareTo(a.getPoints()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public StandingDTO getStandingById(String id) {
        Standing standing = standingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Standing not found"));
        return convertToDTO(standing);
    }

    public StandingDTO createStanding(StandingDTO dto) {
        Championship championship = championshipRepository.findById(dto.getChampionshipId())
                .orElseThrow(() -> new RuntimeException("Championship not found"));
        Team team = teamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new RuntimeException("Team not found"));

        Standing standing = Standing.builder()
                .championship(championship)
                .team(team)
                .wins(0)
                .losses(0)
                .points(0)
                .build();
        standing = standingRepository.save(standing);
        return convertToDTO(standing);
    }

    public StandingDTO updateStanding(String id, StandingDTO dto) {
        Standing standing = standingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Standing not found"));
        standing.setWins(dto.getWins());
        standing.setLosses(dto.getLosses());
        standing.setPoints(dto.getPoints());
        standing = standingRepository.save(standing);
        return convertToDTO(standing);
    }

    public void deleteStanding(String id) {
        standingRepository.deleteById(id);
    }

    public void ensureStandingsExist(String championshipId, String teamAId, String teamBId) {
        createIfNotExists(championshipId, teamAId);
        createIfNotExists(championshipId, teamBId);
    }

    public void updateStandingsForMatch(Match match) {
        String championshipId = match.getChampionship().getId();
        String teamAId = match.getTeamA().getId();
        String teamBId = match.getTeamB().getId();

        Standing standingA = standingRepository.findByChampionshipIdAndTeamId(championshipId, teamAId)
                .orElse(null);
        Standing standingB = standingRepository.findByChampionshipIdAndTeamId(championshipId, teamBId)
                .orElse(null);

        if (standingA == null || standingB == null) return;

        int scoreA = match.getScoreA() != null ? match.getScoreA() : 0;
        int scoreB = match.getScoreB() != null ? match.getScoreB() : 0;

        if (scoreA == 0 && scoreB == 0) return;

        Championship championship = match.getChampionship();
        int pointsPerWin = championship.getPointsPerWin() != null ? championship.getPointsPerWin() : 2;
        int pointsPerLoss = championship.getPointsPerLoss() != null ? championship.getPointsPerLoss() : 0;
        int pointsPerLossDecider = championship.getPointsPerLossDecider() != null ? championship.getPointsPerLossDecider() : 1;
        int bestOf = championship.getBestOf() != null ? championship.getBestOf() : 3;

        boolean isDecider = (scoreA + scoreB) == bestOf;

        if (scoreA > scoreB) {
            standingA.setWins(standingA.getWins() + 1);
            standingA.setPoints(standingA.getPoints() + pointsPerWin);
            standingB.setLosses(standingB.getLosses() + 1);
            standingB.setPoints(standingB.getPoints() + (isDecider ? pointsPerLossDecider : pointsPerLoss));
        } else if (scoreB > scoreA) {
            standingB.setWins(standingB.getWins() + 1);
            standingB.setPoints(standingB.getPoints() + pointsPerWin);
            standingA.setLosses(standingA.getLosses() + 1);
            standingA.setPoints(standingA.getPoints() + (isDecider ? pointsPerLossDecider : pointsPerLoss));
        }

        standingRepository.save(standingA);
        standingRepository.save(standingB);
    }

    public void resetStandingsForMatch(Match match) {
        String championshipId = match.getChampionship().getId();
        String teamAId = match.getTeamA().getId();
        String teamBId = match.getTeamB().getId();

        Standing standingA = standingRepository.findByChampionshipIdAndTeamId(championshipId, teamAId)
                .orElse(null);
        Standing standingB = standingRepository.findByChampionshipIdAndTeamId(championshipId, teamBId)
                .orElse(null);

        if (standingA == null || standingB == null) return;

        int scoreA = match.getScoreA() != null ? match.getScoreA() : 0;
        int scoreB = match.getScoreB() != null ? match.getScoreB() : 0;

        if (scoreA == 0 && scoreB == 0) return;

        Championship championship = match.getChampionship();
        int pointsPerWin = championship.getPointsPerWin() != null ? championship.getPointsPerWin() : 2;
        int pointsPerLoss = championship.getPointsPerLoss() != null ? championship.getPointsPerLoss() : 0;
        int pointsPerLossDecider = championship.getPointsPerLossDecider() != null ? championship.getPointsPerLossDecider() : 1;
        int bestOf = championship.getBestOf() != null ? championship.getBestOf() : 3;

        boolean isDecider = (scoreA + scoreB) == bestOf;

        if (scoreA > scoreB) {
            standingA.setWins(Math.max(0, standingA.getWins() - 1));
            standingA.setPoints(Math.max(0, standingA.getPoints() - pointsPerWin));
            standingB.setLosses(Math.max(0, standingB.getLosses() - 1));
            standingB.setPoints(Math.max(0, standingB.getPoints() - (isDecider ? pointsPerLossDecider : pointsPerLoss)));
        } else if (scoreB > scoreA) {
            standingB.setWins(Math.max(0, standingB.getWins() - 1));
            standingB.setPoints(Math.max(0, standingB.getPoints() - pointsPerWin));
            standingA.setLosses(Math.max(0, standingA.getLosses() - 1));
            standingA.setPoints(Math.max(0, standingA.getPoints() - (isDecider ? pointsPerLossDecider : pointsPerLoss)));
        }

        standingRepository.save(standingA);
        standingRepository.save(standingB);
    }

    private void createIfNotExists(String championshipId, String teamId) {
        if (standingRepository.findByChampionshipIdAndTeamId(championshipId, teamId).isEmpty()) {
            Championship championship = championshipRepository.findById(championshipId).orElseThrow();
            Team team = teamRepository.findById(teamId).orElseThrow();
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

    public void deleteByChampionshipIdAndTeamId(String championshipId, String teamId) {
        standingRepository.findByChampionshipIdAndTeamId(championshipId, teamId)
                .ifPresent(standingRepository::delete);
    }

    public void recalculateStandings(String championshipId) {
        Championship championship = championshipRepository.findById(championshipId)
                .orElseThrow(() -> new RuntimeException("Championship not found"));

        // Reset all standings to zero
        List<Standing> standings = standingRepository.findByChampionshipId(championshipId);
        for (Standing standing : standings) {
            standing.setWins(0);
            standing.setLosses(0);
            standing.setPoints(0);
        }
        standingRepository.saveAll(standings);

        // Recalculate from all matches
        List<Match> matches = matchRepository.findByChampionshipId(championshipId);
        int pointsPerWin = championship.getPointsPerWin() != null ? championship.getPointsPerWin() : 2;
        int pointsPerLoss = championship.getPointsPerLoss() != null ? championship.getPointsPerLoss() : 0;
        int pointsPerLossDecider = championship.getPointsPerLossDecider() != null ? championship.getPointsPerLossDecider() : 1;
        int bestOf = championship.getBestOf() != null ? championship.getBestOf() : 3;

        for (Match match : matches) {
            int scoreA = match.getScoreA() != null ? match.getScoreA() : 0;
            int scoreB = match.getScoreB() != null ? match.getScoreB() : 0;
            if (scoreA == 0 && scoreB == 0) continue;

            String teamAId = match.getTeamA().getId();
            String teamBId = match.getTeamB().getId();

            Standing standingA = standingRepository.findByChampionshipIdAndTeamId(championshipId, teamAId).orElse(null);
            Standing standingB = standingRepository.findByChampionshipIdAndTeamId(championshipId, teamBId).orElse(null);
            if (standingA == null || standingB == null) continue;

            boolean isDecider = (scoreA + scoreB) == bestOf;

            if (scoreA > scoreB) {
                standingA.setWins(standingA.getWins() + 1);
                standingA.setPoints(standingA.getPoints() + pointsPerWin);
                standingB.setLosses(standingB.getLosses() + 1);
                standingB.setPoints(standingB.getPoints() + (isDecider ? pointsPerLossDecider : pointsPerLoss));
            } else if (scoreB > scoreA) {
                standingB.setWins(standingB.getWins() + 1);
                standingB.setPoints(standingB.getPoints() + pointsPerWin);
                standingA.setLosses(standingA.getLosses() + 1);
                standingA.setPoints(standingA.getPoints() + (isDecider ? pointsPerLossDecider : pointsPerLoss));
            }

            standingRepository.save(standingA);
            standingRepository.save(standingB);
        }
    }

    public List<String> getTeamIdsByChampionshipId(String championshipId) {
        return standingRepository.findByChampionshipId(championshipId).stream()
                .map(s -> s.getTeam().getId())
                .collect(Collectors.toList());
    }

    private StandingDTO convertToDTO(Standing standing) {
        return StandingDTO.builder()
                .id(standing.getId())
                .championshipId(standing.getChampionship().getId())
                .teamId(standing.getTeam().getId())
                .wins(standing.getWins())
                .losses(standing.getLosses())
                .points(standing.getPoints())
                .build();
    }
}
