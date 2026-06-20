package com.pact.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    List<CheckIn> findByGoalIdAndMemberIdOrderByCheckInDateDesc(UUID goalId, UUID memberId);

    Optional<CheckIn> findByGoalIdAndMemberIdAndCheckInDate(UUID goalId, UUID memberId, LocalDate checkInDate);

    List<CheckIn> findByGoalIdInAndMemberId(List<UUID> goalIds, UUID memberId);
}