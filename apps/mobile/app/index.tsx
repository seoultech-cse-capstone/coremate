import { router } from "expo-router";
import { LinearGradient } from "expo-linear-gradient";
import { Image, Pressable, StyleSheet, Text, View } from "react-native";

export default function StartScreen() {
  return (
    <LinearGradient
      colors={["#AD04F7", "#0760FD"]}
      start={{ x: 0, y: 0 }}
      end={{ x: 1, y: 1 }}
      style={styles.container}
    >
      <View style={styles.content}>
        <Image
          source={require("../assets/images/splash-icon-white.png")}
          style={styles.logo}
          resizeMode="contain"
        />

        <View style={styles.buttons}>
          <Pressable
            style={styles.emailButton}
            onPress={() => router.push("/login")}
          >
            <Text style={styles.emailButtonText}>이메일로 로그인</Text>
          </Pressable>

          <Pressable style={styles.kakaoButton} onPress={() => {}}>
            <Text style={styles.kakaoButtonText}>카카오 로그인</Text>
          </Pressable>
        </View>
      </View>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 24,
  },
  logo: {
    width: 160,
    height: 160,
    marginBottom: 84,
  },
  buttons: {
    width: "100%",
    gap: 12,
    marginTop: 12,
  },
  emailButton: {
    alignItems: "center",
    borderRadius: 16,
    backgroundColor: "#ffffff",
    paddingVertical: 16,
    shadowColor: "#000000",
    shadowOpacity: 0.14,
    shadowRadius: 16,
    shadowOffset: {
      width: 0,
      height: 8,
    },
    elevation: 6,
  },
  emailButtonText: {
    color: "#0f172a",
    fontSize: 16,
    fontWeight: "800",
  },
  kakaoButton: {
    alignItems: "center",
    borderRadius: 16,
    backgroundColor: "#FEE500",
    paddingVertical: 16,
  },
  kakaoButtonText: {
    color: "#191919",
    fontSize: 16,
    fontWeight: "800",
  },
});
