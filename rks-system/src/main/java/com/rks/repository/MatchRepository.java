package com.rks.repository;

import com.rks.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, String> {
    List<Match> findByChampionshipId(String championshipId);
    List<Match> findByTeamAIdOrTeamBId(String teamAId, String teamBId);
    List<Match> findByChampionshipIdAndTeamAIdOrTeamBId(String championshipId, String teamAId, String teamBId);
}
