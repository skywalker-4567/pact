package com.pact.goal;

import com.pact.common.ApiException;
import com.pact.goal.dto.GoalDtos.*;
import com.pact.squad.Squad;
import com.pact.squad.SquadMembershipRepository;
import com.pact.squad.SquadRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final CheckInRepository checkInRepository;
    private final SquadRepository squadRepository;
    private final SquadMembershipRepository membershipRepository;
    private final StreakCalculator streakCalculator;

    public GoalService(
            GoalRepository goalRepository,
            CheckInRepository checkInRepository,
            SquadRepository squadRepository,
            SquadMembershipRepository membershipRepository,
            StreakCalculator streakCalculator
    ) {
        this.goalRepository = goalRepository;
        this.checkInRepository = checkInRepository;
        this.squadRepository = squadRepository;
        this.membershipRepository = membershipRepository;
        this.streakCalculator = streakCalculator;
    }

    @Transactional
    public GoalCreateResponse createGoal(UUID callerId, UUID squadId, CreateGoalRequest request) {
        requireSquadMember(callerId, squadId);

        Goal goal = new Goal();
        goal.setSquadId(squadId);
        goal.setOwnerId(request.shared() ? null : callerId);
        goal.setTitle(request.title());
        goal.setFrequency("daily");

        Goal saved = goalRepository.save(goal);

        return new GoalCreateResponse(saved.getId(), saved.getTitle(), saved.getOwnerId(), saved.getFrequency());
    }

    public List<GoalSummary> listGoals(UUID callerId, UUID squadId) {
        requireSquadMember(callerId, squadId);

        List<Goal> goals = goalRepository.findBySquadId(squadId);

        return goals.stream()
                .map(goal -> {
                    List<LocalDate> datesDesc = checkInRepository
                            .findByGoalIdAndMemberIdOrderByCheckInDateDesc(goal.getId(), callerId)
                            .stream()
                            .map(CheckIn::getCheckInDate)
                            .toList();

                    StreakCalculator.CurrentStreakResult result =
                            streakCalculator.currentStreak(datesDesc, LocalDate.now());

                    return new GoalSummary(
                            goal.getId(),
                            goal.getTitle(),
                            goal.getOwnerId(),
                            result.currentStreak(),
                            result.lastCheckIn()
                    );
                })
                .toList();
    }

    @Transactional
    public CheckInResult recordCheckIn(UUID callerId, UUID goalId, CheckInRequest request) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "GOAL_NOT_FOUND", "No goal with that id."));

        if (!membershipRepository.existsById_SquadIdAndId_MemberId(goal.getSquadId(), callerId)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "NOT_SQUAD_MEMBER", "You are not a member of this goal's squad.");
        }

        if (goal.getOwnerId() != null && !goal.getOwnerId().equals(callerId)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "NOT_GOAL_OWNER", "Only the goal's owner can check in against it.");
        }

        var existing = checkInRepository.findByGoalIdAndMemberIdAndCheckInDate(
                goalId, callerId, request.date());

        CheckIn checkIn;
        boolean alreadyExisted = existing.isPresent();

        if (alreadyExisted) {
            checkIn = existing.get();
        } else {
            CheckIn fresh = new CheckIn();
            fresh.setGoalId(goalId);
            fresh.setMemberId(callerId);
            fresh.setCheckInDate(request.date());
            checkIn = checkInRepository.save(fresh);
        }

        List<LocalDate> datesDesc = checkInRepository
                .findByGoalIdAndMemberIdOrderByCheckInDateDesc(goalId, callerId)
                .stream()
                .map(CheckIn::getCheckInDate)
                .toList();

        StreakCalculator.CurrentStreakResult result =
                streakCalculator.currentStreak(datesDesc, LocalDate.now());

        CheckInResponse response = new CheckInResponse(
                checkIn.getId(), checkIn.getCheckInDate(), result.currentStreak());

        return new CheckInResult(response, alreadyExisted);
    }

    private void requireSquadMember(UUID callerId, UUID squadId) {
        Squad squad = squadRepository.findById(squadId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "SQUAD_NOT_FOUND", "No squad with that id."));

        if (!membershipRepository.existsById_SquadIdAndId_MemberId(squad.getId(), callerId)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "NOT_SQUAD_MEMBER", "You are not a member of this squad.");
        }
    }

    public record CheckInResult(CheckInResponse response, boolean alreadyExisted) {
    }
}