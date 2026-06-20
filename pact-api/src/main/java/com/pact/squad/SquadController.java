package com.pact.squad;

import com.pact.squad.dto.SquadDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/squads")
public class SquadController {

    private final SquadService squadService;

    public SquadController(SquadService squadService) {
        this.squadService = squadService;
    }

    @PostMapping
    public ResponseEntity<SquadCreateResponse> createSquad(
            @AuthenticationPrincipal UUID memberId,
            @Valid @RequestBody CreateSquadRequest request
    ) {
        SquadDetail detail = squadService.createSquad(memberId, request);
        SquadCreateResponse response = new SquadCreateResponse(detail.id(), detail.name(), detail.inviteCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/join")
    public ResponseEntity<SquadJoinResponse> joinSquad(
            @AuthenticationPrincipal UUID memberId,
            @Valid @RequestBody JoinSquadRequest request
    ) {
        Squad squad = squadService.joinSquad(memberId, request);
        return ResponseEntity.ok(new SquadJoinResponse(squad.getId(), squad.getName()));
    }

    @GetMapping
    public ResponseEntity<List<SquadSummary>> listMySquads(@AuthenticationPrincipal UUID memberId) {
        return ResponseEntity.ok(squadService.listMySquads(memberId));
    }

    @GetMapping("/{squadId}")
    public ResponseEntity<SquadDetail> getSquadDetail(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID squadId
    ) {
        return ResponseEntity.ok(squadService.getSquadDetail(memberId, squadId));
    }

    // POST /api/squads response shape per spec: { id, name, inviteCode } — no members array
    public record SquadCreateResponse(UUID id, String name, String inviteCode) {
    }

    // POST /api/squads/join response shape per spec: { id, name }
    public record SquadJoinResponse(UUID id, String name) {
    }
}