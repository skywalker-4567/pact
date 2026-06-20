package com.pact.squad;

import com.pact.common.ApiException;
import com.pact.member.Member;
import com.pact.member.MemberRepository;
import com.pact.squad.dto.SquadDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SquadService {

    private final SquadRepository squadRepository;
    private final SquadMembershipRepository membershipRepository;
    private final MemberRepository memberRepository;
    private final InviteCodeGenerator inviteCodeGenerator;

    public SquadService(
            SquadRepository squadRepository,
            SquadMembershipRepository membershipRepository,
            MemberRepository memberRepository,
            InviteCodeGenerator inviteCodeGenerator
    ) {
        this.squadRepository = squadRepository;
        this.membershipRepository = membershipRepository;
        this.memberRepository = memberRepository;
        this.inviteCodeGenerator = inviteCodeGenerator;
    }

    @Transactional
    public SquadDetail createSquad(UUID creatorId, CreateSquadRequest request) {
        Squad squad = new Squad();
        squad.setName(request.name());
        squad.setInviteCode(inviteCodeGenerator.generateUniqueCode());
        Squad saved = squadRepository.save(squad);

        SquadMembership membership = new SquadMembership(saved.getId(), creatorId);
        membershipRepository.save(membership);

        Member creator = memberRepository.findById(creatorId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "MEMBER_NOT_FOUND", "Authenticated member not found."));

        return new SquadDetail(
                saved.getId(),
                saved.getName(),
                saved.getInviteCode(),
                List.of(new MemberInfo(creator.getId(), creator.getDisplayName()))
        );
    }

    public Squad joinSquad(UUID memberId, JoinSquadRequest request) {
        Squad squad = squadRepository.findByInviteCode(request.inviteCode())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "SQUAD_NOT_FOUND", "No squad with that invite code."));

        if (membershipRepository.existsById_SquadIdAndId_MemberId(squad.getId(), memberId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "ALREADY_MEMBER", "You are already a member of this squad.");
        }

        membershipRepository.save(new SquadMembership(squad.getId(), memberId));

        return squad;
    }

    public List<SquadSummary> listMySquads(UUID memberId) {
        List<SquadMembership> myMemberships = membershipRepository.findById_MemberId(memberId);

        return myMemberships.stream()
                .map(m -> {
                    UUID squadId = m.getId().getSquadId();
                    Squad squad = squadRepository.findById(squadId)
                            .orElseThrow(() -> new ApiException(
                                    HttpStatus.INTERNAL_SERVER_ERROR, "SQUAD_NOT_FOUND",
                                    "Squad referenced by membership no longer exists."));
                    int memberCount = membershipRepository.findById_SquadId(squadId).size();
                    return new SquadSummary(squad.getId(), squad.getName(), memberCount);
                })
                .collect(Collectors.toList());
    }

    public SquadDetail getSquadDetail(UUID memberId, UUID squadId) {
        Squad squad = squadRepository.findById(squadId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "SQUAD_NOT_FOUND", "No squad with that id."));

        if (!membershipRepository.existsById_SquadIdAndId_MemberId(squadId, memberId)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "NOT_SQUAD_MEMBER", "You are not a member of this squad.");
        }

        List<SquadMembership> memberships = membershipRepository.findById_SquadId(squadId);
        Map<UUID, Member> membersById = memberRepository
                .findAllById(memberships.stream().map(m -> m.getId().getMemberId()).toList())
                .stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        List<MemberInfo> memberInfos = memberships.stream()
                .map(m -> {
                    Member member = membersById.get(m.getId().getMemberId());
                    return new MemberInfo(member.getId(), member.getDisplayName());
                })
                .toList();

        return new SquadDetail(squad.getId(), squad.getName(), squad.getInviteCode(), memberInfos);
    }
}