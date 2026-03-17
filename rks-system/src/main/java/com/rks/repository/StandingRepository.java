package com.rks.repository;

import com.rks.entity.Standing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StandingRepository extends JpaRepository<Standing, String> {
    List<Standing> findByChampionshipId(String championshipId);
    Optional<Standing> findByChampionshipIdAndTeamId(String championshipId, String teamId);
}
