import { apiRequest } from "./client";

export type SquadCreateResponse = {
  id: string;
  name: string;
  inviteCode: string;
};

export type SquadJoinResponse = {
  id: string;
  name: string;
};

export type SquadSummary = {
  id: string;
  name: string;
  memberCount: number;
};

export type SquadMember = {
  id: string;
  displayName: string;
};

export type SquadDetail = {
  id: string;
  name: string;
  inviteCode: string;
  members: SquadMember[];
};

export async function createSquad(name: string): Promise<SquadCreateResponse> {
  return apiRequest<SquadCreateResponse>("/api/squads", {
    method: "POST",
    body: { name },
  });
}

export async function joinSquad(inviteCode: string): Promise<SquadJoinResponse> {
  return apiRequest<SquadJoinResponse>("/api/squads/join", {
    method: "POST",
    body: { inviteCode },
  });
}

export async function listMySquads(): Promise<SquadSummary[]> {
  return apiRequest<SquadSummary[]>("/api/squads", {
    method: "GET",
  });
}

export async function getSquadDetail(squadId: string): Promise<SquadDetail> {
  return apiRequest<SquadDetail>(`/api/squads/${squadId}`, {
    method: "GET",
  });
}