package com.pact.squad.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public class SquadDtos {

    public record CreateSquadRequest(
            @NotBlank String name
    ) {
    }

    public record JoinSquadRequest(
            @NotBlank String inviteCode
    ) {
    }

    public record SquadSummary(
            UUID id,
            String name,
            int memberCount
    ) {
    }

    public record SquadDetail(
            UUID id,
            String name,
            String inviteCode,
            List<MemberInfo> members
    ) {
    }

    public record MemberInfo(
            UUID id,
            String displayName
    ) {
    }
}