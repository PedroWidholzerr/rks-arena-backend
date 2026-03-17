package com.rks.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "championships")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Championship {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @ManyToOne
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ChampionshipStatus status = ChampionshipStatus.UPCOMING;

    @OneToMany(mappedBy = "championship", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Match> matches;

    @OneToMany(mappedBy = "championship", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Standing> standings;

    public enum ChampionshipStatus {
        UPCOMING,
        ONGOING,
        FINISHED
    }
}
