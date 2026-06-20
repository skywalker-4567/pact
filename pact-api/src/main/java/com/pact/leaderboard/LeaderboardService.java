package com.pact.leaderboard;

import com.pact.common.ApiException;
import com.pact.goal.CheckIn;
import com.pact.goal.CheckInRepository;
import com.pact.goal.Goal;
import com.pact.goal.GoalRepository;
import com.pact.goal.StreakCalculator;
import com.pact.leaderboard.dto.LeaderboardEntry;
import com.pact.member.Member;
import com.pact.member.MemberRepository;
import com.pact.squad.SquadMembership;
import com.pact.squad.SquadMembershipRepository;
import com.pact.squad.SquadRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private final SquadRepository squadRepository;
    private final SquadMembershipRepository membershipRepository;
    private final MemberRepository memberRepository;
    private final GoalRepository goalRepository;
    private final CheckInRepository checkInRepository;
    private final StreakCalculator streakCalculator;

    public LeaderboardService(
            SquadRepository squadRepository,
            SquadMembershipRepository membershipRepository,
            MemberRepository memberRepository,
            GoalRepository goalRepository,
            CheckInRepository checkInRepository,
            StreakCalculator streakCalculator
    ) {
        this.squadRepository = squadRepository;
        this.membershipRepository = membershipRepository;
        this.memberRepository = memberRepository;
        this.goalRepository = goalRepository;
        this.checkInRepository = checkInRepository;
        this.streakCalculator = streakCalculator;
    }

    public List<LeaderboardEntry> getLeaderboard(UUID callerId, UUID squadId) {
        squadRepository.findById(squadId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "SQUAD_NOT_FOUND", "No squad with that id."));

        if (!membershipRepository.existsById_SquadIdAndId_MemberId(squadId, callerId)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "NOT_SQUAD_MEMBER", "You are not a member of this squad.");
        }

        List<Goal> squadGoals = goalRepository.findBySquadId(squadId);
        List<UUID> goalIds = squadGoals.stream().map(Goal::getId).toList();

        List<SquadMembership> memberships = membershipRepository.findById_SquadId(squadId);
        List<UUID> memberIds = memberships.stream().map(m -> m.getId().getMemberId()).toList();

        Map<UUID, Member> membersById = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        LocalDate today = LocalDate.now();

        List<LeaderboardEntry> entries = memberIds.stream()
                .map(memberId -> buildEntry(memberId, goalIds, membersById, today))
                .sorted(Comparator.comparingInt(LeaderboardEntry::totalCurrentStreak).reversed())
                .toList();

        return entries;
    }

    private LeaderboardEntry buildEntry(
            UUID memberId,
            List<UUID> goalIds,
            Map<UUID, Member> membersById,
            LocalDate today
    ) {
        if (goalIds.isEmpty()) {
            Member member = membersById.get(memberId);
            return new LeaderboardEntry(memberId, member.getDisplayName(), 0, 0);
        }

        List<CheckIn> allCheckIns = checkInRepository.findByGoalIdInAndMemberId(goalIds, memberId);

        Map<UUID, List<LocalDate>> datesByGoal = allCheckIns.stream()
                .collect(Collectors.groupingBy(
                        CheckIn::getGoalId,
                        Collectors.mapping(CheckIn::getCheckInDate, Collectors.toList())));

        int totalCurrentStreak = 0;
        int longestStreak = 0;

        for (Map.Entry<UUID, List<LocalDate>> entry : datesByGoal.entrySet()) {
            List<LocalDate> dates = entry.getValue();

            List<LocalDate> datesDesc = dates.stream()
                    .sorted(Comparator.reverseOrder())
                    .toList();

            StreakCalculator.CurrentStreakResult currentResult =
                    streakCalculator.currentStreak(datesDesc, today);
            totalCurrentStreak += currentResult.currentStreak();

            int goalLongest = streakCalculator.longestStreak(dates);
            longestStreak = Math.max(longestStreak, goalLongest);
        }

        Member member = membersById.get(memberId);
        return new LeaderboardEntry(memberId, member.getDisplayName(), totalCurrentStreak, longestStreak);
    }
}