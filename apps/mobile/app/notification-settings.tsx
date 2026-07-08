import { StyleSheet, Switch, Text, View } from "react-native";

import { FONT } from "../constants/fonts";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

const notificationItems = [
  { label: "오늘의 퀴즈 알림", value: true },
  { label: "친구 콕 찌르기 알림", value: true },
  { label: "학습 리마인드 알림", value: false },
  { label: "보상 및 이벤트 알림", value: false },
];

export default function NotificationSettingsScreen() {
  return (
    <SettingsDetailLayout title="알림 설정">
      <Text style={detailStyles.sectionTitle}>알림</Text>
      <View style={detailStyles.card}>
        {notificationItems.map((item) => (
          <View key={item.label} style={styles.row}>
            <Text style={styles.rowText}>{item.label}</Text>
            <Switch
              thumbColor="#ffffff"
              trackColor={{ false: "#CBD5E1", true: "#6D5DFB" }}
              value={item.value}
            />
          </View>
        ))}
      </View>
    </SettingsDetailLayout>
  );
}

const styles = StyleSheet.create({
  row: {
    minHeight: 54,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  rowText: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "600",
  },
});
