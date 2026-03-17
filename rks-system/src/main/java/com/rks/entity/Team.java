package com.rks.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "teams")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String tag;

    @Column
    private String description;

    @Column
    private String logoUrl;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Player> players;

    @OneToMany(mappedBy = "teamA")
    private List<Match> matchesAsTeamA;

    @OneToMany(mappedBy = "teamB")
    private List<Match> matchesAsTeamB;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Standing> standings;
}
