package com.pact.squad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SquadMembershipRepository
        extends JpaRepository<SquadMembership, SquadMembership.SquadMembershipId> {

    boolean existsById_SquadIdAndId_MemberId(UUID squadId, UUID memberId);

    List<SquadMembership> findById_SquadId(UUID squadId);

    List<SquadMembership> findById_MemberId(UUID memberId);
}