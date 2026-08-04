import { router } from "expo-router";
import {
  Bell,
  ChevronLeft,
  ChevronRight,
  FileText,
  Globe,
  KeyRound,
  LogOut,
  Pencil,
  Shield,
  Smartphone,
  UserCog,
  UserX,
  type LucideIcon,
} from "lucide-react-native";
import { useState } from "react";
import {
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useLogoutMutation } from "../src/features/auth";

type SettingRoute =
  | "/edit-profile"
  | "/edit-nickname"
  | "/change-password"
  | "/device-management"
  | "/notification-settings"
  | "/language-settings"
  | "/terms-policies"
  | "/privacy-policy"
  | "/delete-account";

type SettingItem = {
  label: string;
  Icon: LucideIcon;
  danger?: boolean;
} & (
  | {
      route: SettingRoute;
      action?: never;
    }
  | {
      action: "logout";
      route?: never;
    }
);

type SettingSection = {
  title: string;
  items: SettingItem[];
};

const settingSections: SettingSection[] = [
  {
    title: "계정",
    items: [
      { label: "회원정보 수정", Icon: UserCog, route: "/edit-profile" },
      { label: "닉네임 수정", Icon: Pencil, route: "/edit-nickname" },
      { label: "비밀번호 변경", Icon: KeyRound, route: "/change-password" },
      { label: "기기 관리", Icon: Smartphone, route: "/device-management" },
    ],
  },
  {
    title: "서비스",
    items: [
      { label: "알림 설정", Icon: Bell, route: "/notification-settings" },
      { label: "언어 설정", Icon: Globe, route: "/language-settings" },
      { label: "약관 및 정책", Icon: FileText, route: "/terms-policies" },
      { label: "개인정보 처리방침", Icon: Shield, route: "/privacy-policy" },
    ],
  },
  {
    title: "계정 관리",
    items: [
      { label: "로그아웃", Icon: LogOut, action: "logout" },
      { label: "회원탈퇴", Icon: UserX, danger: true, route: "/delete-account" },
    ],
  },
];

export default function SettingsScreen() {
  const [logoutVisible, setLogoutVisible] = useState(false);
  const logoutMutation = useLogoutMutation();

  const handleLogout = () => {
    if (logoutMutation.isPending) {
      return;
    }
    logoutMutation.mutate(undefined, {
      onSettled: () => {
        setLogoutVisible(false);
        router.replace("/");
      },
    });
  };

  const handlePress = (item: SettingItem) => {
    if (item.action === "logout") {
      setLogoutVisible(true);
      return;
    }

    router.push(item.route);
  };

  return (
    <View style={styles.screen}>
      <SafeAreaView edges={["top"]} style={styles.safeArea}>
        <View style={styles.container}>
          <View style={styles.headerArea}>
            <Pressable
              accessibilityLabel="뒤로가기"
              accessibilityRole="button"
              hitSlop={10}
              style={styles.backButton}
              onPress={() => router.back()}
            >
              <ChevronLeft color="#111827" size={28} strokeWidth={2.3} />
            </Pressable>

            <Text style={styles.title}>Settings</Text>
          </View>

          <ScrollView
            contentContainerStyle={styles.content}
            showsVerticalScrollIndicator={false}
          >
            {settingSections.map((section) => (
              <View key={section.title} style={styles.section}>
                <Text style={styles.sectionTitle}>{section.title}</Text>

                <View style={styles.card}>
                  {section.items.map((item, index) => {
                    const Icon = item.Icon;
                    const isDanger = Boolean(item.danger);

                    return (
                      <Pressable
                        key={item.label}
                        style={[
                          styles.settingRow,
                          index < section.items.length - 1 && styles.rowDivider,
                        ]}
                        onPress={() => handlePress(item)}
                      >
                        <View
                          style={[
                            styles.iconBox,
                            isDanger && styles.dangerIconBox,
                          ]}
                        >
                          <Icon
                            color={isDanger ? "#EF4444" : "#6D5DFB"}
                            size={21}
                            strokeWidth={2.2}
                          />
                        </View>

                        <Text
                          style={[
                            styles.settingText,
                            isDanger && styles.dangerText,
                          ]}
                        >
                          {item.label}
                        </Text>

                        <ChevronRight
                          color={isDanger ? "#FCA5A5" : "#94a3b8"}
                          size={21}
                          strokeWidth={2.2}
                        />
                      </Pressable>
                    );
                  })}
                </View>
              </View>
            ))}
          </ScrollView>
        </View>
      </SafeAreaView>

      <Modal
        animationType="fade"
        transparent
        visible={logoutVisible}
        onRequestClose={() => setLogoutVisible(false)}
      >
        <View style={styles.modalDim}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>로그아웃할까요?</Text>
            <Text style={styles.modalDescription}>
              다시 이용하려면 로그인 화면에서 계정 정보를 입력해야 합니다.
            </Text>

            <View style={styles.modalActions}>
              <Pressable
                style={styles.cancelButton}
                onPress={() => setLogoutVisible(false)}
              >
                <Text style={styles.cancelButtonText}>취소</Text>
              </Pressable>

              <Pressable
                style={styles.logoutButton}
                onPress={handleLogout}
                disabled={logoutMutation.isPending}
              >
                <Text style={styles.logoutButtonText}>
                  {logoutMutation.isPending ? "로그아웃 중…" : "로그아웃"}
                </Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#ffffff",
  },
  safeArea: {
    flex: 1,
    backgroundColor: "transparent",
  },
  container: {
    flex: 1,
    backgroundColor: "#ffffff",
  },
  headerArea: {
    position: "relative",
    minHeight: 64,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#ffffff",
    paddingHorizontal: 56,
  },
  backButton: {
    position: "absolute",
    left: 12,
    top: 11,
    alignItems: "center",
    justifyContent: "center",
    width: 42,
    height: 42,
  },
  title: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "700",
    textAlign: "center",
  },
  content: {
    paddingHorizontal: 20,
    paddingTop: 18,
    paddingBottom: 36,
  },
  section: {
    marginBottom: 22,
  },
  sectionTitle: {
    marginBottom: 9,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  card: {
    overflow: "hidden",
    borderRadius: 18,
    backgroundColor: "#F8FAFC",
    borderWidth: 1,
    borderColor: "#EEF2F7",
  },
  settingRow: {
    minHeight: 62,
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
  },
  rowDivider: {
    borderBottomWidth: 1,
    borderBottomColor: "#E5E7EB",
  },
  iconBox: {
    alignItems: "center",
    justifyContent: "center",
    width: 38,
    height: 38,
    borderRadius: 13,
    backgroundColor: "rgba(109, 93, 251, 0.12)",
  },
  dangerIconBox: {
    backgroundColor: "rgba(239, 68, 68, 0.1)",
  },
  settingText: {
    flex: 1,
    marginLeft: 12,
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "600",
  },
  dangerText: {
    color: "#EF4444",
  },
  modalDim: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(15, 23, 42, 0.35)",
    paddingHorizontal: 28,
  },
  modalCard: {
    width: "100%",
    borderRadius: 20,
    backgroundColor: "#ffffff",
    padding: 20,
  },
  modalTitle: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "800",
  },
  modalDescription: {
    marginTop: 8,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    lineHeight: 21,
  },
  modalActions: {
    flexDirection: "row",
    gap: 10,
    marginTop: 20,
  },
  cancelButton: {
    flex: 1,
    minHeight: 48,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 15,
    backgroundColor: "#F1F5F9",
  },
  cancelButtonText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
  logoutButton: {
    flex: 1,
    minHeight: 48,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 15,
    backgroundColor: "#6D5DFB",
  },
  logoutButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
});
