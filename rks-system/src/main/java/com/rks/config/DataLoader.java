package com.rks.config;

import com.rks.entity.*;
import com.rks.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;

@Configuration
public class DataLoader {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private ChampionshipRepository championshipRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private StandingRepository standingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner loadData() {
        return args -> {
            // Create default admin user
            if (userRepository.count() == 0) {
                User admin = User.builder()
                        .email("admin@example.com")
                        .password(passwordEncoder.encode("admin123"))
                        .name("Admin User")
                        .role(User.UserRole.ADMIN)
                        .build();
                userRepository.save(admin);
                System.out.println("✓ Admin user created: admin@example.com / admin123");
            }

            // Create seasons
            if (seasonRepository.count() == 0) {
                Season season1 = seasonRepository.save(Season.builder()
                        .name("Season 1: Genesis")
                        .year(2024)
                        .build());

                Season season2 = seasonRepository.save(Season.builder()
                        .name("Season 2: Rising")
                        .year(2025)
                        .build());

                System.out.println("✓ Seasons created");

                // Create championships
                Championship championship1 = championshipRepository.save(Championship.builder()
                        .name("ARAM Cup #1")
                        .description("The inaugural ARAM championship")
                        .season(season1)
                        .status(Championship.ChampionshipStatus.FINISHED)
                        .build());

                Championship championship2 = championshipRepository.save(Championship.builder()
                        .name("ARAM Cup #2")
                        .description("Second ARAM championship")
                        .season(season1)
                        .status(Championship.ChampionshipStatus.ONGOING)
                        .build());

                Championship championship3 = championshipRepository.save(Championship.builder()
                        .name("ARAM Cup #3")
                        .description("Third ARAM championship")
                        .season(season2)
                        .status(Championship.ChampionshipStatus.UPCOMING)
                        .build());

                System.out.println("✓ Championships created");

                // Create teams
                Team team1 = teamRepository.save(Team.builder()
                        .name("Shadow Wolves")
                        .tag("SHW")
                        .description("Aggressive early-game team")
                        .build());

                Team team2 = teamRepository.save(Team.builder()
                        .name("Phoenix Rising")
                        .tag("PHX")
                        .description("Late-game scaling specialists")
                        .build());

                Team team3 = teamRepository.save(Team.builder()
                        .name("Titan Force")
                        .tag("TIF")
                        .description("Balanced all-rounder team")
                        .build());

                System.out.println("✓ Teams created");

                // Create players
                Player player1 = playerRepository.save(Player.builder()
                        .nickname("Darkflow")
                        .tag("DRK")
                        .team(team1)
                        .isCaptain(true)
                        .notes("Team captain")
                        .build());

                Player player2 = playerRepository.save(Player.builder()
                        .nickname("Phoenix")
                        .tag("PHX")
                        .team(team2)
                        .isCaptain(true)
                        .notes("Team captain")
                        .build());

                Player player3 = playerRepository.save(Player.builder()
                        .nickname("Titan")
                        .tag("TTN")
                        .team(team3)
                        .isCaptain(true)
                        .notes("Team captain")
                        .build());

                System.out.println("✓ Players created");

                // Create standings
                Standing standing1_1 = standingRepository.save(Standing.builder()
                        .championship(championship1)
                        .team(team1)
                        .wins(2)
                        .losses(0)
                        .points(6)
                        .build());

                Standing standing1_2 = standingRepository.save(Standing.builder()
                        .championship(championship1)
                        .team(team2)
                        .wins(1)
                        .losses(1)
                        .points(3)
                        .build());

                Standing standing1_3 = standingRepository.save(Standing.builder()
                        .championship(championship1)
                        .team(team3)
                        .wins(0)
                        .losses(2)
                        .points(0)
                        .build());

                System.out.println("✓ Standings created");

                // Create matches
                Match match1 = matchRepository.save(Match.builder()
                        .championship(championship1)
                        .teamA(team1)
                        .teamB(team2)
                        .scheduledAt(LocalDateTime.of(2025, 3, 1, 20, 0, 0))
                        .scoreA(1)
                        .scoreB(0)
                        .comment("Great match!")
                        .mvpPlayer(player1)
                        .build());

                Match match2 = matchRepository.save(Match.builder()
                        .championship(championship1)
                        .teamA(team1)
                        .teamB(team3)
                        .scheduledAt(LocalDateTime.of(2025, 3, 2, 20, 0, 0))
                        .scoreA(2)
                        .scoreB(1)
                        .comment("Close game")
                        .mvpPlayer(player1)
                        .build());

                Match match3 = matchRepository.save(Match.builder()
                        .championship(championship2)
                        .teamA(team2)
                        .teamB(team3)
                        .scheduledAt(LocalDateTime.of(2025, 3, 10, 20, 0, 0))
                        .scoreA(0)
                        .scoreB(0)
                        .comment("Upcoming match")
                        .build());

                System.out.println("✓ Matches created");

                System.out.println("\n" +
                        "✅ Database populated with sample data!\n" +
                        "🔐 Login credentials:\n" +
                        "   Email: admin@example.com\n" +
                        "   Password: admin123\n" +
                        "🚀 API running at: http://localhost:8080\n"
                );
            }
        };
    }
}
