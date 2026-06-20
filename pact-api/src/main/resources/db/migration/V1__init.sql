-- Enable pgcrypto so gen_random_uuid() is available
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE member (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name  TEXT        NOT NULL,
    email         TEXT        NOT NULL UNIQUE,
    password_hash TEXT        NOT NULL,
    device_token  TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE squad (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT        NOT NULL,
    invite_code TEXT        NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE squad_membership (
    squad_id  UUID        NOT NULL REFERENCES squad(id),
    member_id UUID        NOT NULL REFERENCES member(id),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (squad_id, member_id)
);

CREATE TABLE goal (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    squad_id   UUID        NOT NULL REFERENCES squad(id),
    owner_id   UUID        REFERENCES member(id),   -- null = shared squad goal
    title      TEXT        NOT NULL,
    frequency  TEXT        NOT NULL DEFAULT 'daily',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE check_in (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id        UUID NOT NULL REFERENCES goal(id),
    member_id      UUID NOT NULL REFERENCES member(id),
    check_in_date  DATE NOT NULL,                    -- client-supplied local date
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (goal_id, member_id, check_in_date)
);

-- Explicit indexes for FK columns queried directly
CREATE INDEX idx_goal_squad_id ON goal(squad_id);
CREATE INDEX idx_check_in_goal_id ON check_in(goal_id);
CREATE INDEX idx_check_in_member_id ON check_in(member_id);
CREATE INDEX idx_squad_membership_member_id ON squad_membership(member_id);