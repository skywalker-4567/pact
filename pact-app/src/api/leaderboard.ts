import { apiRequest } from "./client";

export type LeaderboardEntry = {
  memberId: string;
  displayName: string;
  totalCurrentStreak: number;
  longestStreak: number;
};

export async function getLeaderboard(squadId: string): Promise<LeaderboardEntry[]> {
  return apiRequest<LeaderboardEntry[]>(`/api/squads/${squadId}/leaderboard`, {
    method: "GET",
  });
}