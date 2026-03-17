package com.rks.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String tag;

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private Boolean isCaptain = false;

    @Column
    private String notes;

    @OneToMany(mappedBy = "mvpPlayer")
    private List<Match> mvpMatches;
}
