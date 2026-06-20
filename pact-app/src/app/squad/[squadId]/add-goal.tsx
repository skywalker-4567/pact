import React, { useState } from "react";
import { View, Text, TextInput, Pressable, Switch, StyleSheet, ActivityIndicator } from "react-native";
import { router, useLocalSearchParams } from "expo-router";
import { createGoal } from "../../../src/api/goals";
import { ApiError } from "../../../src/api/client";

export default function AddGoalScreen() {
  const { squadId } = useLocalSearchParams<{ squadId: string }>();

  const [title, setTitle] = useState("");
  const [shared, setShared] = useState(false); // default: personal goal
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleSubmit() {
    setErrorMessage(null);
    setIsLoading(true);
    try {
      await createGoal(squadId, title.trim(), shared);
      router.replace(`/squad/${squadId}`);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage("Something went wrong. Please try again.");
      }
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Add a goal</Text>

      <TextInput
        style={styles.input}
        placeholder="Goal title"
        value={title}
        onChangeText={setTitle}
      />

      <View style={styles.switchRow}>
        <View style={styles.switchLabelGroup}>
          <Text style={styles.switchLabel}>Shared with the squad</Text>
          <Text style={styles.switchSubtitle}>
            {shared
              ? "Everyone in the squad can check in, each with their own streak."
              : "Only you can check in to this goal."}
          </Text>
        </View>
        <Switch value={shared} onValueChange={setShared} />
      </View>

      {errorMessage && <Text style={styles.error}>{errorMessage}</Text>}

      <Pressable
        style={[styles.button, isLoading && styles.buttonDisabled]}
        onPress={handleSubmit}
        disabled={isLoading}
      >
        {isLoading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>Create goal</Text>
        )}
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    padding: 24,
    backgroundColor: "#fff",
  },
  title: {
    fontSize: 28,
    fontWeight: "600",
    marginBottom: 24,
  },
  input: {
    borderWidth: 1,
    borderColor: "#ccc",
    borderRadius: 8,
    padding: 12,
    marginBottom: 20,
    fontSize: 16,
  },
  switchRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 24,
  },
  switchLabelGroup: {
    flex: 1,
    paddingRight: 12,
  },
  switchLabel: {
    fontSize: 16,
    fontWeight: "600",
    marginBottom: 4,
  },
  switchSubtitle: {
    fontSize: 13,
    color: "#666",
  },
  button: {
    backgroundColor: "#2563eb",
    borderRadius: 8,
    padding: 14,
    alignItems: "center",
    marginTop: 8,
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "600",
  },
  error: {
    color: "#dc2626",
    marginBottom: 12,
  },
});