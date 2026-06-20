package com.pact.leaderboard;

import com.pact.leaderboard.dto.LeaderboardEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/api/squads/{squadId}/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID squadId
    ) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(memberId, squadId));
    }
}