import { StyleSheet, Text, View } from "react-native";

import { FONT } from "../../constants/fonts";

export default function MyPageScreen() {
  return (
    <View style={styles.container}>
      <View style={styles.titleArea}>
        <Text style={styles.title}>마이페이지</Text>
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
    height: 148,
    justifyContent: "center",
    paddingHorizontal: 24,
  },
  title: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 28,
    fontWeight: "800",
  },
  panel: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    width: "100%",
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: "#F3F3F3",
    paddingHorizontal: 24,
  },
  description: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 15,
  },
});
