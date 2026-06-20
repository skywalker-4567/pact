package com.pact.goal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public class GoalDtos {

    public record CreateGoalRequest(
            @NotBlank String title,
            boolean shared
    ) {
    }

    public record GoalCreateResponse(
            UUID id,
            String title,
            UUID ownerId,
            String frequency
    ) {
    }

    public record GoalSummary(
            UUID id,
            String title,
            UUID ownerId,
            int currentStreak,
            LocalDate lastCheckIn
    ) {
    }

    public record CheckInRequest(
            @NotNull LocalDate date
    ) {
    }

    public record CheckInResponse(
            UUID id,
            LocalDate checkInDate,
            int currentStreak
    ) {
    }
}