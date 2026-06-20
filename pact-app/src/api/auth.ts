import { apiRequest } from "./client";

export type MemberSummary = {
  id: string;
  displayName: string;
  email: string;
};

export type AuthResponse = {
  token: string;
  member: MemberSummary;
};

export async function register(
  email: string,
  password: string,
  displayName: string
): Promise<AuthResponse> {
  return apiRequest<AuthResponse>("/api/auth/register", {
    method: "POST",
    body: { email, password, displayName },
    authenticated: false,
  });
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  return apiRequest<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: { email, password },
    authenticated: false,
  });
}