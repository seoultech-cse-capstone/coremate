import { StyleSheet, Text, View } from "react-native";

import { FONT } from "../../constants/fonts";

export default function MyPageScreen() {
  return (
    <View style={styles.container}>
      <View style={styles.titleArea}>
        <Text style={styles.title}>My Page</Text>
      </View>

      <View style={styles.panel}>
        <Text style={styles.description}>마이페이지 화면을 준비 중입니다.</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#6D5DFB",
  },
  titleArea: {
    minHeight: 82,
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "rgba(109, 93, 251, 0.8)",
    paddingHorizontal: 16,
    paddingTop: 8,
  },
  title: {
    flexShrink: 1,
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 24,
    fontWeight: "700",
  },
  panel: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    width: "100%",
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    backgroundColor: "#F3F3F3",
    paddingHorizontal: 24,
  },
  description: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 15,
  },
});
