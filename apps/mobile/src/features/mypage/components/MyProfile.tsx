import { Image, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../../constants/fonts";
import { useAuth } from "../../auth";
import { useMyCurrency } from "../../currency";
import { getLevelProgress, useMyProfile } from "../../user";

export function MyProfile() {
  const { accessToken } = useAuth();
  const { data: profile } = useMyProfile(accessToken);
  const { data: currency } = useMyCurrency(accessToken);

  const level = profile?.level ?? 1;
  const totalXp = profile?.totalXp ?? 0;
  const levelProgress = getLevelProgress(level, totalXp);

  const stats = [
    { label: "Level", value: `Lv.${level}` },
    { label: "Day Streak", value: `${profile?.attendanceCount ?? 0}` },
    { label: "Coins", value: `${currency?.coin ?? 0}` },
  ];

  return (
    <View style={styles.container}>
      <View style={styles.userRow}>
        <Image
          source={require("../../../../assets/images/splash-icon-white.png")}
          style={styles.avatar}
          resizeMode="cover"
        />

        <View style={styles.userText}>
          <Text style={styles.nickname}>{profile?.nickname ?? "코어메이트"}</Text>
          <Text style={styles.school}>{profile?.schoolName ?? "학교 미등록"}</Text>
          {profile?.lastAttendanceDate ? (
            <Text style={styles.school}>
              최근 출석 {profile.lastAttendanceDate.slice(0, 10).replace(/-/g, ".")}
            </Text>
          ) : null}

          <View style={styles.xpArea}>
            <View style={styles.xpTextRow}>
              <Text style={styles.xpText}>
                {levelProgress.isMaxLevel
                  ? "MAX"
                  : `${levelProgress.currentLevelXp.toLocaleString()} / ${levelProgress.requiredLevelXp.toLocaleString()}`}
              </Text>
              <Text style={styles.xpText}>{levelProgress.percent}%</Text>
            </View>

            <View style={styles.xpTrack}>
              <View style={[styles.xpFill, { width: `${levelProgress.percent}%` }]} />
            </View>
          </View>
        </View>
      </View>

      <View style={styles.statsRow}>
        {stats.map((item) => (
          <View style={styles.statBox} key={item.label}>
            <Text style={styles.statValue}>{item.value}</Text>
            <Text style={styles.statLabel}>{item.label}</Text>
          </View>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 20,
  },
  userRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 16,
  },
  avatar: {
    width: 76,
    height: 76,
    borderRadius: 38,
    backgroundColor: "rgba(255, 255, 255, 0.2)",
  },
  userText: {
    flex: 1,
    gap: 6,
  },
  nickname: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 22,
    fontWeight: "800",
  },
  school: {
    color: "rgba(255, 255, 255, 0.82)",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
  },
  xpArea: {
    marginTop: 2,
    gap: 5,
  },
  xpTextRow: {
    flexDirection: "row",
    justifyContent: "space-between",
  },
  xpText: {
    color: "rgba(255, 255, 255, 0.82)",
    fontFamily: FONT.kufam,
    fontSize: 11,
    fontWeight: "600",
  },
  xpTrack: {
    height: 6,
    borderRadius: 3,
    backgroundColor: "rgba(255, 255, 255, 0.24)",
    overflow: "hidden",
  },
  xpFill: {
    height: "100%",
    borderRadius: 3,
    backgroundColor: "#2EE6A6",
  },
  statsRow: {
    flexDirection: "row",
    gap: 10,
  },
  statBox: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    minHeight: 70,
    borderRadius: 16,
    backgroundColor: "rgba(255, 255, 255, 0.16)",
    borderWidth: 1,
    borderColor: "rgba(255, 255, 255, 0.22)",
    paddingHorizontal: 8,
  },
  statValue: {
    color: "#ffffff",
    fontFamily: FONT.kufam,
    fontSize: 18,
    fontWeight: "700",
  },
  statLabel: {
    marginTop: 4,
    color: "rgba(255, 255, 255, 0.75)",
    fontFamily: FONT.kufam,
    fontSize: 11,
    fontWeight: "500",
    textAlign: "center",
  },
});
