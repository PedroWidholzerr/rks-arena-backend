package com.rks.repository;

import com.rks.entity.Championship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChampionshipRepository extends JpaRepository<Championship, String> {
    List<Championship> findBySeasonId(String seasonId);
}
