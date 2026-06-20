package com.pact.goal;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreakCalculatorTest {

    private final StreakCalculator calculator = new StreakCalculator();

    @Test
    void emptyHistory_returnsZeroStreakAndNullLastCheckIn() {
        StreakCalculator.CurrentStreakResult result =
                calculator.currentStreak(List.of(), LocalDate.of(2026, 6, 19));

        assertThat(result.currentStreak()).isEqualTo(0);
        assertThat(result.lastCheckIn()).isNull();
    }

    @Test
    void checkedInToday_currentStreakIsOne() {
        LocalDate today = LocalDate.of(2026, 6, 19);
        StreakCalculator.CurrentStreakResult result =
                calculator.currentStreak(List.of(today), today);

        assertThat(result.currentStreak()).isEqualTo(1);
        assertThat(result.lastCheckIn()).isEqualTo(today);
    }

    @Test
    void checkedInYesterdayOnly_gracePeriodKeepsStreakAlive() {
        LocalDate today = LocalDate.of(2026, 6, 19);
        LocalDate yesterday = today.minusDays(1);

        StreakCalculator.CurrentStreakResult result =
                calculator.currentStreak(List.of(yesterday), today);

        assertThat(result.currentStreak()).isEqualTo(1);
        assertThat(result.lastCheckIn()).isEqualTo(yesterday);
    }

    @Test
    void lastCheckInTwoOrMoreDaysAgo_streakResetsToZero_butLastCheckInStillReported() {
        LocalDate today = LocalDate.of(2026, 6, 19);
        LocalDate twoDaysAgo = today.minusDays(2);

        StreakCalculator.CurrentStreakResult result =
                calculator.currentStreak(List.of(twoDaysAgo), today);

        assertThat(result.currentStreak()).isEqualTo(0);
        assertThat(result.lastCheckIn()).isEqualTo(twoDaysAgo);
    }

    @Test
    void fiveConsecutiveDaysEndingToday_currentStreakIsFive() {
        LocalDate today = LocalDate.of(2026, 6, 19);
        List<LocalDate> datesDesc = List.of(
                today,
                today.minusDays(1),
                today.minusDays(2),
                today.minusDays(3),
                today.minusDays(4)
        );

        StreakCalculator.CurrentStreakResult result = calculator.currentStreak(datesDesc, today);

        assertThat(result.currentStreak()).isEqualTo(5);
        assertThat(result.lastCheckIn()).isEqualTo(today);
    }

    @Test
    void brokenRunWithGapInMiddle_currentStreakOnlyCountsBackToGap() {
        LocalDate today = LocalDate.of(2026, 6, 19);
        // today, yesterday, day-before-yesterday consecutive, then a gap, then older dates
        List<LocalDate> datesDesc = List.of(
                today,
                today.minusDays(1),
                today.minusDays(2),
                today.minusDays(5), // gap here — breaks the run
                today.minusDays(6)
        );

        StreakCalculator.CurrentStreakResult result = calculator.currentStreak(datesDesc, today);

        assertThat(result.currentStreak()).isEqualTo(3);
        assertThat(result.lastCheckIn()).isEqualTo(today);
    }

    @Test
    void longestStreak_findsBestHistoricalRun_evenWhenNotTheCurrentOne() {
        LocalDate today = LocalDate.of(2026, 6, 19);

        // Current run: just today (length 1).
        // Historical run further back: 6 consecutive days (length 6) — the longest.
        List<LocalDate> allDates = List.of(
                today,
                today.minusDays(10),
                today.minusDays(11),
                today.minusDays(12),
                today.minusDays(13),
                today.minusDays(14),
                today.minusDays(15)
        );

        int longest = calculator.longestStreak(allDates);

        assertThat(longest).isEqualTo(6);
    }

    @Test
    void longestStreak_emptyHistory_returnsZero() {
        assertThat(calculator.longestStreak(List.of())).isEqualTo(0);
    }
}