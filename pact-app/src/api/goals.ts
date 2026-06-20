import { apiRequest } from "./client";

export type GoalCreateResponse = {
  id: string;
  title: string;
  ownerId: string | null;
  frequency: string;
};

export type GoalSummary = {
  id: string;
  title: string;
  ownerId: string | null;
  currentStreak: number;
  lastCheckIn: string | null;
};

export type CheckInResponse = {
  id: string;
  checkInDate: string;
  currentStreak: number;
};

/**
 * Formats a Date as the device's local YYYY-MM-DD, zero-padded.
 * Deliberately does NOT use Date.toISOString(), which reports the UTC date
 * and can be off by one near local midnight depending on the device's
 * timezone — the spec requires the client's actual local date.
 */
export function toLocalDateString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export async function createGoal(
  squadId: string,
  title: string,
  shared: boolean
): Promise<GoalCreateResponse> {
  return apiRequest<GoalCreateResponse>(`/api/squads/${squadId}/goals`, {
    method: "POST",
    body: { title, shared },
  });
}

export async function listGoals(squadId: string): Promise<GoalSummary[]> {
  return apiRequest<GoalSummary[]>(`/api/squads/${squadId}/goals`, {
    method: "GET",
  });
}

export async function recordCheckIn(goalId: string, date: string): Promise<CheckInResponse> {
  return apiRequest<CheckInResponse>(`/api/goals/${goalId}/check-ins`, {
    method: "POST",
    body: { date },
  });
}