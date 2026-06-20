package com.pact.squad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SquadRepository extends JpaRepository<Squad, UUID> {

    Optional<Squad> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}