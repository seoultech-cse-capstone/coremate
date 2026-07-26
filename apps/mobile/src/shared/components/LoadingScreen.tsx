import { ActivityIndicator, StyleSheet, View } from "react-native";

const BRAND_PURPLE = "#6D5DFB";

// 흰 화면 + 보라색 스피너. 토큰 확인 등 네트워크 대기가 필요한 화면에서
// 빈 화면이나 어색한 깜빡임 대신 공통으로 쓴다.
export function LoadingScreen() {
  return (
    <View style={styles.screen}>
      <ActivityIndicator color={BRAND_PURPLE} size="large" />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#ffffff",
  },
});
