import { StyleSheet, Text, View } from "react-native";

export default function HomeScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>CoreMate</Text>
      <Text style={styles.description}>홈 화면을 준비 중입니다.</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 24,
    backgroundColor: "#f8fafc",
  },
  title: {
    color: "#6D5DFB",
    fontSize: 32,
    fontWeight: "900",
  },
  description: {
    marginTop: 10,
    color: "#64748b",
    fontSize: 15,
  },
});
