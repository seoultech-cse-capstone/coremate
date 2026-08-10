import { ActivityIndicator, StyleSheet, Switch, Text, View } from "react-native";

import { FONT } from "../constants/fonts";
import {
  useNotificationSettingsQuery,
  useUpdateNotificationSettingMutation,
} from "../src/features/notification";
import type { NotificationCategory } from "../src/features/notification/types";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

/**
 * 카테고리별 표시 이름.
 *
 * '친구 알림'은 콕 찌르기뿐 아니라 친구 요청·수락도 함께 묶인다. 예전 이름이
 * '친구 콕 찌르기 알림'이라 요청 알림까지 이 스위치로 꺼지는 것이 드러나지 않았다.
 * 계정 보안 알림(SYSTEM)은 끌 수 없어 서버가 목록에 내려주지 않는다.
 */
const CATEGORY_LABELS: Record<NotificationCategory, string> = {
  DAILY: "오늘의 퀴즈·출석 알림",
  FRIEND: "친구 알림",
  LEARNING: "학습 리마인드 알림",
  REWARD: "보상 및 이벤트 알림",
};

export default function NotificationSettingsScreen() {
  const { data: settings = [], isLoading } = useNotificationSettingsQuery();
  const updateSetting = useUpdateNotificationSettingMutation();

  return (
    <SettingsDetailLayout title="알림 설정">
      <Text style={detailStyles.sectionTitle}>알림</Text>
      <View style={detailStyles.card}>
        {isLoading ? (
          <View style={styles.loading}>
            <ActivityIndicator color="#6D5DFB" />
          </View>
        ) : (
          settings.map((setting) => (
            <View key={setting.category} style={styles.row}>
              <Text style={styles.rowText}>{CATEGORY_LABELS[setting.category]}</Text>
              <Switch
                onValueChange={(enabled) =>
                  updateSetting.mutate({ category: setting.category, enabled })
                }
                thumbColor="#ffffff"
                trackColor={{ false: "#CBD5E1", true: "#6D5DFB" }}
                value={setting.enabled}
              />
            </View>
          ))
        )}
      </View>
      <Text style={styles.help}>
        기기 설정에서 알림을 꺼 두면 이 설정과 관계없이 알림이 오지 않습니다.
      </Text>
    </SettingsDetailLayout>
  );
}

const styles = StyleSheet.create({
  loading: { paddingVertical: 32 },
  row: {
    minHeight: 54,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 16,
  },
  rowText: {
    color: "#1F2937",
    fontFamily: FONT.pretendard,
    fontSize: 14,
  },
  help: {
    color: "#94A3B8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    lineHeight: 17,
    paddingHorizontal: 4,
    paddingTop: 10,
  },
});
