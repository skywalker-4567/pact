import React, { useCallback, useEffect, useState } from "react";
import {
  View,
  Text,
  FlatList,
  Pressable,
  StyleSheet,
  ActivityIndicator,
} from "react-native";
import { router, useFocusEffect } from "expo-router";
import { useAuth } from "../src/auth/AuthContext";
import { listMySquads, SquadSummary } from "../src/api/squads";
import { ApiError } from "../src/api/client";

export default function HomeScreen() {
  const { member, logout } = useAuth();
  const [squads, setSquads] = useState<SquadSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadSquads = useCallback(async () => {
    setErrorMessage(null);
    setIsLoading(true);
    try {
      const result = await listMySquads();
      setSquads(result);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage("Something went wrong. Please try again.");
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Reload every time this screen comes into focus, so squads created or
  // joined elsewhere show up without a manual refresh.
  useFocusEffect(
    useCallback(() => {
      loadSquads();
    }, [loadSquads])
  );

  function formatMemberCount(count: number): string {
    return count === 1 ? "1 member" : `${count} members`;
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Welcome, {member?.displayName ?? "friend"}</Text>
        <Pressable onPress={logout}>
          <Text style={styles.logout}>Log out</Text>
        </Pressable>
      </View>

      {isLoading ? (
        <View style={styles.centerFill}>
          <ActivityIndicator size="large" />
        </View>
      ) : errorMessage ? (
        <View style={styles.centerFill}>
          <Text style={styles.error}>{errorMessage}</Text>
          <Pressable style={styles.retryButton} onPress={loadSquads}>
            <Text style={styles.retryButtonText}>Try again</Text>
          </Pressable>
        </View>
      ) : squads.length === 0 ? (
        <View style={styles.centerFill}>
          <Text style={styles.emptyTitle}>No squads yet</Text>
          <Text style={styles.emptySubtitle}>
            Create a squad or join one with an invite code to get started.
          </Text>
        </View>
      ) : (
        <FlatList
          data={squads}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          renderItem={({ item }) => (
            <Pressable
              style={styles.squadRow}
              onPress={() => router.push(`/squad/${item.id}`)}
            >
              <Text style={styles.squadName}>{item.name}</Text>
              <Text style={styles.squadMemberCount}>{formatMemberCount(item.memberCount)}</Text>
            </Pressable>
          )}
        />
      )}

      <View style={styles.actions}>
        <Pressable style={styles.button} onPress={() => router.push("/create-squad")}>
          <Text style={styles.buttonText}>Create a squad</Text>
        </Pressable>
        <Pressable
          style={[styles.button, styles.secondaryButton]}
          onPress={() => router.push("/join-squad")}
        >
          <Text style={[styles.buttonText, styles.secondaryButtonText]}>Join a squad</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
    padding: 24,
  },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 16,
  },
  title: {
    fontSize: 22,
    fontWeight: "600",
    flexShrink: 1,
  },
  logout: {
    color: "#dc2626",
    fontWeight: "600",
  },
  centerFill: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingHorizontal: 16,
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
  list: {
    paddingBottom: 16,
  },
  squadRow: {
    borderWidth: 1,
    borderColor: "#eee",
    borderRadius: 8,
    padding: 16,
    marginBottom: 12,
  },
  squadName: {
    fontSize: 16,
    fontWeight: "600",
    marginBottom: 4,
  },
  squadMemberCount: {
    fontSize: 14,
    color: "#666",
  },
  actions: {
    marginTop: 16,
    gap: 12,
  },
  button: {
    backgroundColor: "#2563eb",
    borderRadius: 8,
    padding: 14,
    alignItems: "center",
  },
  buttonText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "600",
  },
  secondaryButton: {
    backgroundColor: "#fff",
    borderWidth: 1,
    borderColor: "#2563eb",
  },
  secondaryButtonText: {
    color: "#2563eb",
  },
});