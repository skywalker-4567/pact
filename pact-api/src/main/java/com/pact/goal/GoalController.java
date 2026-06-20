package com.pact.goal;

import com.pact.goal.dto.GoalDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping("/api/squads/{squadId}/goals")
    public ResponseEntity<GoalCreateResponse> createGoal(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID squadId,
            @Valid @RequestBody CreateGoalRequest request
    ) {
        GoalCreateResponse response = goalService.createGoal(memberId, squadId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/squads/{squadId}/goals")
    public ResponseEntity<List<GoalSummary>> listGoals(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID squadId
    ) {
        return ResponseEntity.ok(goalService.listGoals(memberId, squadId));
    }

    @PostMapping("/api/goals/{goalId}/check-ins")
    public ResponseEntity<CheckInResponse> recordCheckIn(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID goalId,
            @Valid @RequestBody CheckInRequest request
    ) {
        GoalService.CheckInResult result = goalService.recordCheckIn(memberId, goalId, request);
        HttpStatus status = result.alreadyExisted() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.response());
    }
}