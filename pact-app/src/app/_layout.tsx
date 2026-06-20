import React, { useEffect } from "react";
import { Stack, router, useSegments } from "expo-router";
import { ActivityIndicator, View } from "react-native";
import { AuthProvider, useAuth } from "../auth/AuthContext";

function RootNavigation() {
  const { token, isRestoring } = useAuth();
  const segments = useSegments();

  useEffect(() => {
    if (isRestoring) return;

    const inAuthGroup = segments[0] === "login" || segments[0] === "register";

    if (!token && !inAuthGroup) {
      router.replace("/login");
    } else if (token && inAuthGroup) {
      router.replace("/");
    }
  }, [token, isRestoring, segments]);

  if (isRestoring) {
    return (
      <View style={{ flex: 1, justifyContent: "center", alignItems: "center" }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return <Stack screenOptions={{ headerShown: false }} />;
}

export default function RootLayout() {
  return (
    <AuthProvider>
      <RootNavigation />
    </AuthProvider>
  );
}