import React, { useCallback, useState } from "react";
import { View, Text, FlatList, Pressable, StyleSheet, ActivityIndicator } from "react-native";
import { useLocalSearchParams, useFocusEffect } from "expo-router";
import { getLeaderboard, LeaderboardEntry } from "../../../src/api/leaderboard";
import { ApiError } from "../../../src/api/client";
import { useAuth } from "../../../src/auth/AuthContext";

export default function LeaderboardScreen() {
  const { squadId } = useLocalSearchParams<{ squadId: string }>();
  const { member } = useAuth();

  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadLeaderboard = useCallback(async () => {
    setErrorMessage(null);
    setIsLoading(true);
    try {
      const result = await getLeaderboard(squadId);
      setEntries(result);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage("Something went wrong. Please try again.");
      }
    } finally {
      setIsLoading(false);
    }
  }, [squadId]);

  useFocusEffect(
    useCallback(() => {
      loadLeaderboard();
    }, [loadLeaderboard])
  );

  if (isLoading) {
    return (
      <View style={styles.centerFill}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (errorMessage) {
    return (
      <View style={styles.centerFill}>
        <Text style={styles.error}>{errorMessage}</Text>
        <Pressable style={styles.retryButton} onPress={loadLeaderboard}>
          <Text style={styles.retryButtonText}>Try again</Text>
        </Pressable>
      </View>
    );
  }

  if (entries.length === 0) {
    return (
      <View style={styles.centerFill}>
        <Text style={styles.emptyTitle}>No streaks yet</Text>
        <Text style={styles.emptySubtitle}>
          Once members start checking in, rankings will show up here.
        </Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={entries}
        keyExtractor={(item) => item.memberId}
        contentContainerStyle={styles.list}
        ListHeaderComponent={<Text style={styles.sectionTitle}>Leaderboard</Text>}
        renderItem={({ item, index }) => {
          const rank = index + 1;
          const isCurrentMember = item.memberId === member?.id;

          return (
            <View style={[styles.row, isCurrentMember && styles.rowHighlighted]}>
              <Text style={styles.rank}>{rank}</Text>

              <View style={styles.nameAndStreaks}>
                <Text style={styles.displayName}>
                  {item.displayName}
                  {isCurrentMember ? " (You)" : ""}
                </Text>
                <Text style={styles.longestStreak}>Best streak: {item.longestStreak}</Text>
              </View>

              <Text style={styles.totalStreak}>{item.totalCurrentStreak}</Text>
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
  sectionTitle: {
    fontSize: 24,
    fontWeight: "700",
    marginBottom: 16,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: "600",
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 14,
    color: "#666",
    textAlign: "center",
  },
  error: {
    color: "#dc2626",
    textAlign: "center",
    marginBottom: 12,
  },
  retryButton: {
    backgroundColor: "#2563eb",
    borderRadius: 8,
    paddingVertical: 10,
    paddingHorizontal: 20,
  },
  retryButtonText: {
    color: "#fff",
    fontWeight: "600",
  },
  row: {
    borderWidth: 1,
    borderColor: "#eee",
    borderRadius: 8,
    padding: 16,
    marginBottom: 12,
    flexDirection: "row",
    alignItems: "center",
  },
  rowHighlighted: {
    backgroundColor: "#eff6ff",
    borderColor: "#2563eb",
  },
  rank: {
    fontSize: 18,
    fontWeight: "700",
    color: "#888",
    width: 32,
  },
  nameAndStreaks: {
    flex: 1,
    paddingHorizontal: 8,
  },
  displayName: {
    fontSize: 16,
    fontWeight: "600",
    marginBottom: 2,
  },
  longestStreak: {
    fontSize: 12,
    color: "#888",
  },
  totalStreak: {
    fontSize: 22,
    fontWeight: "700",
    color: "#16a34a",
  },
});