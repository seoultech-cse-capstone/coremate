import { router } from "expo-router";
import { LinearGradient } from "expo-linear-gradient";
import { Mail, MessageCircle } from "lucide-react-native";
import { Image, Pressable, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import Svg, { Path } from "react-native-svg";

import { FONT } from "../constants/fonts";
import { useAuth } from "../src/features/auth";

function GoogleLogo({ size = 20 }: { size?: number }) {
  return (
    <Svg viewBox="0 0 48 48" width={size} height={size}>
      <Path
        fill="#FFC107"
        d="M43.611,20.083H42V20H24v8h11.303c-1.649,4.657-6.08,8-11.303,8c-6.627,0-12-5.373-12-12
        c0-6.627,5.373-12,12-12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C12.955,4,4,12.955,4,24
        c0,11.045,8.955,20,20,20c11.045,0,20-8.955,20-20C44,22.659,43.862,21.35,43.611,20.083z"
      />
      <Path
        fill="#FF3D00"
        d="M6.306,14.691l6.571,4.819C14.655,15.108,18.961,12,24,12c3.059,0,5.842,1.154,7.961,3.039
        l5.657-5.657C34.046,6.053,29.268,4,24,4C16.318,4,9.656,8.337,6.306,14.691z"
      />
      <Path
        fill="#4CAF50"
        d="M24,44c5.166,0,9.86-1.977,13.409-5.192l-6.19-5.238C29.211,35.091,26.715,36,24,36
        c-5.202,0-9.619-3.317-11.283-7.946l-6.522,5.025C9.505,39.556,16.227,44,24,44z"
      />
      <Path
        fill="#1976D2"
        d="M43.611,20.083H42V20H24v8h11.303c-0.792,2.237-2.231,4.166-4.087,5.571
        c0.001-0.001,0.002-0.001,0.003-0.002l6.19,5.238C36.971,39.205,44,34,44,24C44,22.659,43.862,21.35,43.611,20.083z"
      />
    </Svg>
  );
}

export default function StartScreen() {
  const { continueAsGuest } = useAuth();

  const handleGuest = () => {
    continueAsGuest();
    router.replace("/(tabs)/content");
  };

  return (
    <LinearGradient
      colors={["#AD04F7", "#0760FD"]}
      start={{ x: 0, y: 0 }}
      end={{ x: 1, y: 1 }}
      style={styles.container}
    >
      <SafeAreaView edges={["top"]} style={styles.content}>
        <Image
          source={require("../assets/images/splash-icon-white.png")}
          style={styles.logo}
          resizeMode="contain"
        />

        <View style={styles.buttons}>
          <Pressable style={styles.kakaoButton} onPress={() => {}}>
            <MessageCircle color="#191919" size={20} strokeWidth={2.3} />
            <Text style={styles.kakaoButtonText}>카카오 로그인</Text>
          </Pressable>

          <Pressable style={styles.googleButton} onPress={() => {}}>
            <GoogleLogo size={20} />
            <Text style={styles.googleButtonText}>구글 로그인</Text>
          </Pressable>

          <Pressable
            style={styles.emailButton}
            onPress={() => router.push("/email-login")}
          >
            <Mail color="#0f172a" size={20} strokeWidth={2.3} />
            <Text style={styles.emailButtonText}>이메일로 로그인</Text>
          </Pressable>

          <Pressable style={styles.guestButton} onPress={handleGuest}>
            <Text style={styles.guestButtonText}>비로그인으로 체험하기</Text>
          </Pressable>
        </View>
      </SafeAreaView>
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
  kakaoButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    borderRadius: 16,
    backgroundColor: "#FEE500",
    paddingVertical: 16,
  },
  kakaoButtonText: {
    color: "#191919",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "800",
  },
  googleButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    borderRadius: 16,
    backgroundColor: "#ffffff",
    borderWidth: 1,
    borderColor: "#DADCE0",
    paddingVertical: 16,
  },
  googleButtonText: {
    color: "#3c4043",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "800",
  },
  emailButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    borderRadius: 16,
    backgroundColor: "#ffffff",
    paddingVertical: 16,
    marginTop: 20,
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
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "800",
  },
  guestButton: {
    alignItems: "center",
    paddingVertical: 14,
  },
  guestButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
    textDecorationLine: "underline",
  },
});
