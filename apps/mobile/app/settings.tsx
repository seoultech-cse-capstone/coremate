import { router } from "expo-router";
import {
  Bell,
  ChevronLeft,
  ChevronRight,
  FileText,
  LogOut,
  Pencil,
  Shield,
  UserCog,
  UserX,
} from "lucide-react-native";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";

const settingSections = [
  {
    title: "계정",
    items: [
      { label: "회원정보 수정", Icon: UserCog },
      { label: "닉네임 수정", Icon: Pencil },
    ],
  },
  {
    title: "서비스",
    items: [
      { label: "알림 설정", Icon: Bell },
      { label: "약관 및 정책", Icon: FileText },
      { label: "개인정보 처리방침", Icon: Shield },
    ],
  },
  {
    title: "계정 관리",
    items: [
      { label: "로그아웃", Icon: LogOut },
      { label: "회원탈퇴", Icon: UserX, danger: true },
    ],
  },
];

export default function SettingsScreen() {
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
                  {section.items.map(({ Icon, danger, label }, index) => (
                    <Pressable
                      key={label}
                      style={[
                        styles.settingRow,
                        index < section.items.length - 1 && styles.rowDivider,
                      ]}
                    >
                      <View
                        style={[
                          styles.iconBox,
                          danger && styles.dangerIconBox,
                        ]}
                      >
                        <Icon
                          color={danger ? "#EF4444" : "#6D5DFB"}
                          size={21}
                          strokeWidth={2.2}
                        />
                      </View>

                      <Text
                        style={[
                          styles.settingText,
                          danger && styles.dangerText,
                        ]}
                      >
                        {label}
                      </Text>

                      <ChevronRight
                        color={danger ? "#FCA5A5" : "#94a3b8"}
                        size={21}
                        strokeWidth={2.2}
                      />
                    </Pressable>
                  ))}
                </View>
              </View>
            ))}
          </ScrollView>
        </View>
      </SafeAreaView>
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
});
