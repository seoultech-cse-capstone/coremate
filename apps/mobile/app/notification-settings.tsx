import * as Notifications from "expo-notifications";
import { BellOff, ShieldCheck } from "lucide-react-native";
import { useEffect, useState } from "react";
import {
  ActivityIndicator,
  AppState,
  Linking,
  Pressable,
  StyleSheet,
  Switch,
  Text,
  View,
} from "react-native";

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
 * 카테고리별 이름과 언제 무엇이 오는지. 스위치만 있으면 끄면 무엇이 안 오는지 알 수 없어서(#247)
 * 서버 발송 기준(NotificationScheduler·발송 시점)을 한 줄로 적는다.
 *
 * '친구 알림'은 콕 찌르기뿐 아니라 친구 요청·수락도 함께 묶인다.
 * 계정 보안 알림(SYSTEM)은 끌 수 없어 서버가 목록에 내려주지 않고, 화면 아래에 고정으로 보여 준다.
 */
const CATEGORY_INFO: Record<NotificationCategory, { label: string; description: string }> = {
  DAILY: {
    label: "오늘의 퀴즈·출석",
    description: "아침 8시 퀴즈 오픈, 출석 전이면 밤 9시",
  },
  FRIEND: {
    label: "친구",
    description: "친구 요청·수락, 친구가 콕 찔렀을 때",
  },
  LEARNING: {
    label: "학습 리마인드",
    description: "3일 넘게 쉬었으면 일요일 저녁 7시에",
  },
  REWARD: {
    label: "보상·이벤트",
    description: "레벨이 올랐을 때",
  },
};

/** 기기(OS) 알림 허용 여부. 꺼져 있으면 이 화면의 스위치와 관계없이 알림이 오지 않는다. */
function useDeviceNotificationDenied(): boolean {
  const [denied, setDenied] = useState(false);

  useEffect(() => {
    const check = () => {
      Notifications.getPermissionsAsync()
        .then((permission) => setDenied(permission.status === "denied"))
        .catch((error) => console.error("[notification-settings] 알림 권한 조회 실패", error));
    };
    check();
    // 기기 설정에서 켜고 돌아오면 안내를 바로 지운다.
    const subscription = AppState.addEventListener("change", (state) => {
      if (state === "active") {
        check();
      }
    });
    return () => subscription.remove();
  }, []);

  return denied;
}

export default function NotificationSettingsScreen() {
  const { data: settings = [], isLoading } = useNotificationSettingsQuery();
  const updateSetting = useUpdateNotificationSettingMutation();
  const isDeviceDenied = useDeviceNotificationDenied();

  return (
    <SettingsDetailLayout title="알림 설정">
      {isDeviceDenied ? (
        <View style={styles.deniedBanner}>
          <BellOff color="#B45309" size={20} strokeWidth={2.2} />
          <View style={styles.deniedBody}>
            <Text style={styles.deniedTitle}>기기 알림이 꺼져 있어요</Text>
            <Text style={styles.deniedText}>
              기기 설정에서 CoreMate 알림을 허용해야 아래 알림을 받을 수 있어요.
            </Text>
            <Pressable
              accessibilityRole="button"
              hitSlop={6}
              onPress={() => Linking.openSettings()}
            >
              <Text style={styles.deniedLink}>기기 설정 열기</Text>
            </Pressable>
          </View>
        </View>
      ) : null}

      <Text style={detailStyles.sectionTitle}>받을 알림</Text>
      <View style={[detailStyles.card, styles.card]}>
        {isLoading ? (
          <View style={styles.loading}>
            <ActivityIndicator color="#6D5DFB" />
          </View>
        ) : (
          settings.map((setting, index) => (
            <View
              key={setting.category}
              style={[styles.row, index < settings.length - 1 && styles.rowDivider]}
            >
              <View style={styles.rowTextArea}>
                <Text style={styles.rowLabel}>{CATEGORY_INFO[setting.category].label}</Text>
                <Text style={styles.rowDescription}>
                  {CATEGORY_INFO[setting.category].description}
                </Text>
              </View>
              <Switch
                accessibilityLabel={`${CATEGORY_INFO[setting.category].label} 알림`}
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

      <Text style={detailStyles.sectionTitle}>항상 받는 알림</Text>
      <View style={[detailStyles.card, styles.card]}>
        <View style={styles.row}>
          <View style={styles.rowTextArea}>
            <Text style={styles.rowLabel}>계정 보안</Text>
            <Text style={styles.rowDescription}>
              새 기기에서 로그인했을 때. 계정을 지키기 위해 끌 수 없어요
            </Text>
          </View>
          <ShieldCheck color="#6D5DFB" size={22} strokeWidth={2.2} />
        </View>
      </View>
    </SettingsDetailLayout>
  );
}

const styles = StyleSheet.create({
  card: { padding: 0 },
  loading: { paddingVertical: 32 },
  row: {
    minHeight: 68,
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  rowDivider: {
    borderBottomWidth: 1,
    borderBottomColor: "#EEF2F7",
  },
  rowTextArea: { flex: 1, gap: 3 },
  rowLabel: {
    color: "#1F2937",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "600",
  },
  rowDescription: {
    color: "#64748B",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    lineHeight: 17,
  },
  deniedBanner: {
    flexDirection: "row",
    gap: 10,
    borderRadius: 16,
    backgroundColor: "#FFFBEB",
    borderWidth: 1,
    borderColor: "#FDE68A",
    padding: 14,
    marginBottom: 20,
  },
  deniedBody: { flex: 1, gap: 4 },
  deniedTitle: {
    color: "#92400E",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  deniedText: {
    color: "#92400E",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    lineHeight: 17,
  },
  deniedLink: {
    marginTop: 4,
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
});
