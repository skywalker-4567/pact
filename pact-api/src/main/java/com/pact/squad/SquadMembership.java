package com.pact.squad;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "squad_membership")
public class SquadMembership {

    @EmbeddedId
    private SquadMembershipId id;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    public SquadMembership() {
    }

    public SquadMembership(UUID squadId, UUID memberId) {
        this.id = new SquadMembershipId(squadId, memberId);
    }

    @PrePersist
    void prePersist() {
        if (joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }

    public SquadMembershipId getId() {
        return id;
    }

    public void setId(SquadMembershipId id) {
        this.id = id;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    @Embeddable
    public static class SquadMembershipId implements Serializable {

        @Column(name = "squad_id")
        private UUID squadId;

        @Column(name = "member_id")
        private UUID memberId;

        public SquadMembershipId() {
        }

        public SquadMembershipId(UUID squadId, UUID memberId) {
            this.squadId = squadId;
            this.memberId = memberId;
        }

        public UUID getSquadId() {
            return squadId;
        }

        public void setSquadId(UUID squadId) {
            this.squadId = squadId;
        }

        public UUID getMemberId() {
            return memberId;
        }

        public void setMemberId(UUID memberId) {
            this.memberId = memberId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SquadMembershipId that)) return false;
            return Objects.equals(squadId, that.squadId) && Objects.equals(memberId, that.memberId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(squadId, memberId);
        }
    }
}