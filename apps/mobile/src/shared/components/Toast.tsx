import { Animated, StyleSheet, Text } from "react-native";

import { FONT } from "../../../constants/fonts";

type ToastProps = {
  message: string | null;
  opacity: Animated.Value;
  bottom?: number;
};

export function Toast({ message, opacity, bottom = 32 }: ToastProps) {
  if (!message) {
    return null;
  }

  return (
    <Animated.View
      pointerEvents="none"
      style={[styles.toast, { bottom, opacity }]}
    >
      <Text style={styles.toastText}>{message}</Text>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  toast: {
    position: "absolute",
    left: 24,
    right: 24,
    alignItems: "center",
    borderRadius: 999,
    backgroundColor: "rgba(17, 24, 39, 0.92)",
    paddingVertical: 12,
    paddingHorizontal: 20,
  },
  toastText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
  },
});
