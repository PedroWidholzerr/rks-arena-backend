package com.rks.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "standings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Standing {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private Integer wins = 0;

    @Column(nullable = false)
    private Integer losses = 0;

    @Column(nullable = false)
    private Integer points = 0;
}
