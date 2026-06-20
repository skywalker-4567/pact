package com.pact.leaderboard.dto;

import java.util.UUID;

public record LeaderboardEntry(
        UUID memberId,
        String displayName,
        int totalCurrentStreak,
        int longestStreak
) {
}