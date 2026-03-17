package com.rks.repository;

import com.rks.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, String> {
    Optional<Team> findByTag(String tag);
}
