import React, { useCallback, useState } from "react";
import {
  View,
  Text,
  FlatList,
  Pressable,
  StyleSheet,
  ActivityIndicator,
  Alert,
} from "react-native";
import { router, useLocalSearchParams, useFocusEffect } from "expo-router";
import { getSquadDetail, SquadDetail } from "../../../api/squads";
import {
  listGoals,
  recordCheckIn,
  toLocalDateString,
  GoalSummary,
} from "../../../api/goals";
import { ApiError } from "../../../api/client";
import { useAuth } from "../../../auth/AuthContext";

export default function SquadDetailScreen() {
  const { squadId } = useLocalSearchParams<{ squadId: string }>();
  const { member } = useAuth();

  const [squad, setSquad] = useState<SquadDetail | null>(null);
  const [goals, setGoals] = useState<GoalSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [checkingInGoalId, setCheckingInGoalId] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setErrorMessage(null);
    setIsLoading(true);
    try {
      const [squadDetail, goalList] = await Promise.all([
        getSquadDetail(squadId),
        listGoals(squadId),
      ]);
      setSquad(squadDetail);
      setGoals(goalList);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMessage(err instanceof Error ? err.message : "Something went wrong. Please try again.");
      } else {
        setErrorMessage("Something went wrong. Please try again.");
      }
    } finally {
      setIsLoading(false);
    }
  }, [squadId]);

  useFocusEffect(
    useCallback(() => {
      loadData();
    }, [loadData])
  );

  async function handleCheckIn(goalId: string) {
    setCheckingInGoalId(goalId);
    try {
      const today = toLocalDateString(new Date());
      const result = await recordCheckIn(goalId, today);

      // Update just this goal's streak in place — no full refetch needed,
      // whether this was a fresh check-in (201) or an idempotent repeat (200).
      setGoals((current) =>
        current.map((goal) =>
          goal.id === goalId
            ? { ...goal, currentStreak: result.currentStreak, lastCheckIn: result.checkInDate }
            : goal
        )
      );
    } catch (err) {
      // A native alert, not inline state — visible regardless of scroll position
      // and self-dismissing, so it can't linger after a later successful check-in.
      const message =
        err instanceof ApiError ? err.message : "Something went wrong. Please try again.";
      Alert.alert("Check-in failed", message);
    } finally {
      setCheckingInGoalId(null);
    }
  }

  function ownerDisplayName(ownerId: string): string {
    const owner = squad?.members.find((m) => m.id === ownerId);
    return owner?.displayName ?? "someone";
  }

  if (isLoading) {
    return (
      <View style={styles.centerFill}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (errorMessage && !squad) {
    return (
      <View style={styles.centerFill}>
        <Text style={styles.error}>{errorMessage}</Text>
        <Pressable style={styles.retryButton} onPress={loadData}>
          <Text style={styles.retryButtonText}>Try again</Text>
        </Pressable>
      </View>
    );
  }

  if (!squad) {
    return null;
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={goals}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        ListHeaderComponent={
          <View>
            <Text style={styles.squadName}>{squad.name}</Text>

            <View style={styles.inviteBox}>
              <Text style={styles.inviteLabel}>Invite code</Text>
              <Text style={styles.inviteCode}>{squad.inviteCode}</Text>
            </View>

            <Text style={styles.sectionTitle}>Members</Text>
            <View style={styles.membersRow}>
              {squad.members.map((m) => (
                <View key={m.id} style={styles.memberPill}>
                  <Text style={styles.memberPillText}>{m.displayName}</Text>
                </View>
              ))}
            </View>

            <View style={styles.navRow}>
              <Pressable
                style={styles.navButton}
                onPress={() => router.push(`/squad/${squadId}/add-goal`)}
              >
                <Text style={styles.navButtonText}>+ Add goal</Text>
              </Pressable>
              <Pressable
                style={[styles.navButton, styles.secondaryNavButton]}
                onPress={() => router.push(`/squad/${squadId}/leaderboard`)}
              >
                <Text style={[styles.navButtonText, styles.secondaryNavButtonText]}>
                  Leaderboard
                </Text>
              </Pressable>
            </View>

            {errorMessage && <Text style={styles.error}>{errorMessage}</Text>}

            <Text style={styles.sectionTitle}>Goals</Text>

            {goals.length === 0 && (
              <Text style={styles.emptyText}>No goals yet — add one to get started.</Text>
            )}
          </View>
        }
        renderItem={({ item }) => {
          const isShared = item.ownerId === null;
          const isOwnPersonalGoal = item.ownerId === member?.id;
          const canCheckIn = isShared || isOwnPersonalGoal;
          const isCheckingIn = checkingInGoalId === item.id;

          return (
            <View style={styles.goalRow}>
              <View style={styles.goalInfo}>
                <Text style={styles.goalTitle}>{item.title}</Text>
                <Text style={styles.goalMeta}>
                  {isShared
                    ? "Shared goal"
                    : isOwnPersonalGoal
                    ? "Your personal goal"
                    : `${ownerDisplayName(item.ownerId as string)}'s personal goal`}
                </Text>
                <Text style={styles.goalStreak}>🔥 {item.currentStreak} day streak</Text>
              </View>

              {canCheckIn && (
                <Pressable
                  style={[styles.checkInButton, isCheckingIn && styles.checkInButtonDisabled]}
                  onPress={() => handleCheckIn(item.id)}
                  disabled={isCheckingIn}
                >
                  {isCheckingIn ? (
                    <ActivityIndicator color="#fff" size="small" />
                  ) : (
                    <Text style={styles.checkInButtonText}>Check in today</Text>
                  )}
                </Pressable>
              )}
            </View>
          );
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
  },
  centerFill: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingHorizontal: 16,
  },
  list: {
    padding: 24,
  },
  squadName: {
    fontSize: 24,
    fontWeight: "700",
    marginBottom: 16,
  },
  inviteBox: {
    borderWidth: 1,
    borderColor: "#2563eb",
    borderRadius: 8,
    padding: 12,
    marginBottom: 20,
  },
  inviteLabel: {
    fontSize: 12,
    color: "#666",
    marginBottom: 2,
  },
  inviteCode: {
    fontSize: 20,
    fontWeight: "700",
    color: "#2563eb",
    letterSpacing: 2,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: "600",
    marginBottom: 8,
    marginTop: 8,
  },
  membersRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginBottom: 16,
  },
  memberPill: {
    backgroundColor: "#f1f5f9",
    borderRadius: 16,
    paddingVertical: 6,
    paddingHorizontal: 12,
  },
  memberPillText: {
    fontSize: 13,
    color: "#334155",
  },
  navRow: {
    flexDirection: "row",
    gap: 12,
    marginBottom: 16,
  },
  navButton: {
    flex: 1,
    backgroundColor: "#2563eb",
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: "center",
  },
  navButtonText: {
    color: "#fff",
    fontWeight: "600",
  },
  secondaryNavButton: {
    backgroundColor: "#fff",
    borderWidth: 1,
    borderColor: "#2563eb",
  },
  secondaryNavButtonText: {
    color: "#2563eb",
  },
  emptyText: {
    color: "#666",
    marginBottom: 8,
  },
  error: {
    color: "#dc2626",
    marginBottom: 12,
  },
  retryButton: {
    backgroundColor: "#2563eb",
    borderRadius: 8,
    paddingVertical: 10,
    paddingHorizontal: 20,
    marginTop: 12,
  },
  retryButtonText: {
    color: "#fff",
    fontWeight: "600",
  },
  goalRow: {
    borderWidth: 1,
    borderColor: "#eee",
    borderRadius: 8,
    padding: 16,
    marginBottom: 12,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  goalInfo: {
    flex: 1,
    paddingRight: 12,
  },
  goalTitle: {
    fontSize: 16,
    fontWeight: "600",
    marginBottom: 2,
  },
  goalMeta: {
    fontSize: 12,
    color: "#888",
    marginBottom: 4,
  },
  goalStreak: {
    fontSize: 14,
    color: "#334155",
  },
  checkInButton: {
    backgroundColor: "#16a34a",
    borderRadius: 8,
    paddingVertical: 10,
    paddingHorizontal: 14,
    minWidth: 110,
    alignItems: "center",
  },
  checkInButtonDisabled: {
    opacity: 0.6,
  },
  checkInButtonText: {
    color: "#fff",
    fontWeight: "600",
    fontSize: 13,
  },
});