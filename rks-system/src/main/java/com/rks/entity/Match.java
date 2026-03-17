package com.rks.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @ManyToOne
    @JoinColumn(name = "team_a_id", nullable = false)
    private Team teamA;

    @ManyToOne
    @JoinColumn(name = "team_b_id", nullable = false)
    private Team teamB;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column
    private Integer scoreA = 0;

    @Column
    private Integer scoreB = 0;

    @Column
    private String comment;

    @ManyToOne
    @JoinColumn(name = "mvp_player_id")
    private Player mvpPlayer;
}
