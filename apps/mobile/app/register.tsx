import { router } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";

export default function RegisterScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>회원가입</Text>
      <Text style={styles.description}>
        회원가입 화면은 다음 단계에서 연결할 예정입니다.
      </Text>

      <Pressable style={styles.button} onPress={() => router.back()}>
        <Text style={styles.buttonText}>돌아가기</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    paddingHorizontal: 24,
    backgroundColor: "#f8fafc",
  },
  title: {
    color: "#6D5DFB",
    fontSize: 30,
    fontWeight: "900",
    textAlign: "center",
  },
  description: {
    marginTop: 12,
    color: "#64748b",
    fontSize: 15,
    lineHeight: 22,
    textAlign: "center",
  },
  button: {
    alignItems: "center",
    borderRadius: 16,
    backgroundColor: "#6D5DFB",
    paddingVertical: 16,
    marginTop: 28,
  },
  buttonText: {
    color: "#ffffff",
    fontSize: 15,
    fontWeight: "800",
  },
});
