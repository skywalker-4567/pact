package com.pact.goal;

import java.time.LocalDate;
import java.util.List;

/**
 * Pure streak-math, no Spring dependencies. Dates passed in are assumed
 * already sorted descending (most recent first) where noted — see each method.
 */
public class StreakCalculator {

    public record CurrentStreakResult(int currentStreak, LocalDate lastCheckIn) {
    }

    /**
     * @param checkInDatesDesc check-in dates for one (goal, member) pair, sorted
     *                         descending (most recent first). May contain duplicates
     *                         in theory, but the DB unique constraint prevents that
     *                         in practice — this method does not de-duplicate.
     * @param today            the current date, passed in explicitly for testability.
     */
    public CurrentStreakResult currentStreak(List<LocalDate> checkInDatesDesc, LocalDate today) {
        if (checkInDatesDesc.isEmpty()) {
            return new CurrentStreakResult(0, null);
        }

        LocalDate mostRecent = checkInDatesDesc.get(0);
        long daysSinceMostRecent = java.time.temporal.ChronoUnit.DAYS.between(mostRecent, today);

        if (daysSinceMostRecent >= 2) {
            // Streak lapsed — gap of 2+ days since the last check-in.
            return new CurrentStreakResult(0, mostRecent);
        }

        // mostRecent is today or yesterday — walk backward counting consecutive days.
        int streak = 1;
        LocalDate expectedPrevious = mostRecent.minusDays(1);

        for (int i = 1; i < checkInDatesDesc.size(); i++) {
            LocalDate candidate = checkInDatesDesc.get(i);
            if (candidate.equals(expectedPrevious)) {
                streak++;
                expectedPrevious = candidate.minusDays(1);
            } else if (candidate.isBefore(expectedPrevious)) {
                // Gap found — streak stops here.
                break;
            }
            // candidate.equals(mostRecent) or any duplicate date is skipped without
            // breaking or incrementing, in case of unexpected duplicate rows.
        }

        return new CurrentStreakResult(streak, mostRecent);
    }

    /**
     * @param checkInDates check-in dates for one (goal, member) pair, in any order.
     *                      Independent of "today" — purely historical.
     */
    public int longestStreak(List<LocalDate> checkInDates) {
        if (checkInDates.isEmpty()) {
            return 0;
        }

        List<LocalDate> sortedAsc = checkInDates.stream()
                .sorted()
                .distinct()
                .toList();

        int longest = 1;
        int current = 1;

        for (int i = 1; i < sortedAsc.size(); i++) {
            LocalDate previous = sortedAsc.get(i - 1);
            LocalDate curr = sortedAsc.get(i);

            if (curr.equals(previous.plusDays(1))) {
                current++;
            } else {
                current = 1;
            }
            longest = Math.max(longest, current);
        }

        return longest;
    }
}