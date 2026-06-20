import React, { useState } from "react";
import { View, Text, TextInput, Pressable, StyleSheet, ActivityIndicator } from "react-native";
import { router } from "expo-router";
import { createSquad } from "../api/squads";
import { ApiError } from "../api/client";

export default function CreateSquadScreen() {
  const [name, setName] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [inviteCode, setInviteCode] = useState<string | null>(null);

  async function handleSubmit() {
    setErrorMessage(null);
    setIsLoading(true);
    try {
      const result = await createSquad(name.trim());
      setInviteCode(result.inviteCode);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMessage(err instanceof Error ? err.message : "Something went wrong. Please try again.");
      } else {
        setErrorMessage("Something went wrong. Please try again.");
      }
    } finally {
      setIsLoading(false);
    }
  }

  if (inviteCode) {
    return (
      <View style={styles.container}>
        <Text style={styles.title}>Squad created!</Text>
        <Text style={styles.subtitle}>Share this code with your friends:</Text>

        <View style={styles.codeBox}>
          <Text style={styles.codeText}>{inviteCode}</Text>
        </View>

        <Pressable style={styles.button} onPress={() => router.replace("/")}>
          <Text style={styles.buttonText}>Done</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Create a squad</Text>

      <TextInput
        style={styles.input}
        placeholder="Squad name"
        value={name}
        onChangeText={setName}
      />

      {errorMessage && <Text style={styles.error}>{errorMessage}</Text>}

      <Pressable
        style={[styles.button, isLoading && styles.buttonDisabled]}
        onPress={handleSubmit}
        disabled={isLoading}
      >
        {isLoading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>Create</Text>
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
  subtitle: {
    fontSize: 16,
    color: "#666",
    marginBottom: 24,
  },
  input: {
    borderWidth: 1,
    borderColor: "#ccc",
    borderRadius: 8,
    padding: 12,
    marginBottom: 12,
    fontSize: 16,
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
  codeBox: {
    borderWidth: 2,
    borderColor: "#2563eb",
    borderRadius: 12,
    paddingVertical: 32,
    alignItems: "center",
    marginBottom: 32,
  },
  codeText: {
    fontSize: 40,
    fontWeight: "700",
    letterSpacing: 4,
    color: "#2563eb",
  },
});