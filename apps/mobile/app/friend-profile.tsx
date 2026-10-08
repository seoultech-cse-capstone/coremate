import { useQueryClient } from "@tanstack/react-query";
import { BlurView } from "expo-blur";
import { router, useLocalSearchParams } from "expo-router";
import { ChevronLeft, Flame, Hand, House, MapPin } from "lucide-react-native";
import { useMemo, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useRequireMember } from "../src/features/auth";
import { AvatarFaceThumbnail, toEquippedMap } from "../src/features/avatar";
import { useSubjects } from "../src/features/content";
import {
  isPokeLimitError,
  useBlockUserMutation,
  useDeleteFriendMutation,
  useFriendLearningHistoryQuery,
  useFriendProfileQuery,
  usePokeMutation,
} from "../src/features/friend";
import { getAvatarColor } from "../src/features/friend/avatarColor";
import {
  FriendProgressStats,
  type ProgressAxis,
} from "../src/features/friend/components/FriendProgressStats";
import { FriendRoomModal } from "../src/features/friend/components/FriendRoomModal";
import { getApiErrorCode, getApiErrorMessage } from "../src/shared/utils/apiError";

const BRAND_PURPLE = "#6D5DFB";
const PANEL_GRAY = "#F3F3F3";
const OVERSCROLL_BUFFER = 96;

// SCR-FRIEND-02 화면 상태 중 "친구 관계 아님"·"대상 사용자 탈퇴"는 일반 오류와 따로 알린다.
// 상대가 친구를 끊었는데 내 목록이 아직 갱신되지 않았을 때 주로 만난다.
function getProfileErrorText(error: unknown): string {
  switch (getApiErrorCode(error)) {
    case "FRIEND_RELATION_REQUIRED":
      return "친구 관계가 아니에요";
    case "USER_NOT_FOUND":
    case "ACCOUNT_NOT_ACTIVE":
      return "탈퇴했거나 이용이 중지된 사용자예요";
    default:
      return "친구 정보를 불러오지 못했어요";
  }
}

export default function FriendProfileScreen() {
  useRequireMember();
  const insets = useSafeAreaInsets();
  const { friendId } = useLocalSearchParams<{ friendId?: string }>();
  const friendUserId = Number(friendId);
  const targetUserId = Number.isFinite(friendUserId) ? friendUserId : null;

  const queryClient = useQueryClient();
  const { data: profile, isLoading, isError, error } = useFriendProfileQuery(targetUserId);
  // 학습 기록을 공개하지 않은 친구면 서버가 막는다. 프로필을 받기 전에는 공개로 보고 함께 부른다.
  const { data: learningHistory } = useFriendLearningHistoryQuery(
    targetUserId,
    profile?.learningPublic !== false,
  );
  const { data: subjects = [] } = useSubjects();
  const pokeMutation = usePokeMutation();
  const deleteFriendMutation = useDeleteFriendMutation();
  const blockUserMutation = useBlockUserMutation();

  const [isPoked, setIsPoked] = useState(false);
  const [pokeMessage, setPokeMessage] = useState<string | null>(null);
  const [isPokeBlocked, setIsPokeBlocked] = useState(false);
  const [isRoomModalVisible, setIsRoomModalVisible] = useState(false);

  // 서버는 과목을 id로만 내려준다. 이름은 content API에 있으므로 여기서 붙인다.
  const progressAxes = useMemo<ProgressAxis[]>(() => {
    const nameById = new Map(subjects.map((subject) => [subject.subjectId, subject.subjectName]));
    return (profile?.subjectStats ?? []).map((stat) => ({
      label: nameById.get(stat.subjectId) ?? `과목 ${stat.subjectId}`,
      // achievementRate는 0~100 실수다.
      value: stat.achievementRate ?? 0,
    }));
  }, [profile?.subjectStats, subjects]);

  if (isLoading || !profile) {
    return (
      <View style={[styles.container, styles.centered]}>
        {isError ? (
          <>
            <Text style={styles.errorText}>{getProfileErrorText(error)}</Text>
            <Pressable
              accessibilityRole="button"
              onPress={() => {
                // 관계가 끊겼거나 탈퇴했으면 친구 목록에 남은 카드도 사라지게 다시 불러온다.
                queryClient.invalidateQueries({ queryKey: ["friends"] });
                router.back();
              }}
            >
              <Text style={styles.errorAction}>돌아가기</Text>
            </Pressable>
          </>
        ) : (
          <ActivityIndicator color="#FFFFFF" />
        )}
      </View>
    );
  }

  const avatarColor = getAvatarColor(profile.nickname);
  const isLearningPublic = profile.learningPublic !== false;

  // 콕 찌르기는 취소가 없다. 서버에 기록이 남는 행위라 다시 눌러도 되돌릴 수 없다.
  const poke = () => {
    if (isPoked || isPokeBlocked || targetUserId === null) {
      return;
    }
    pokeMutation.mutate(targetUserId, {
      onSuccess: () => {
        setIsPoked(true);
        setPokeMessage(null);
      },
      onError: (error) => {
        if (isPokeLimitError(error)) {
          setIsPokeBlocked(true);
        }
        setPokeMessage(getApiErrorMessage(error));
      },
    });
  };

  const confirmDelete = () => {
    Alert.alert("친구 삭제", `${profile.nickname} 님을 친구 목록에서 삭제할까요?`, [
      { text: "취소", style: "cancel" },
      {
        text: "삭제",
        style: "destructive",
        onPress: () => {
          if (targetUserId === null) {
            return;
          }
          deleteFriendMutation.mutate(targetUserId, {
            onSuccess: () => router.back(),
            onError: (error) => Alert.alert("친구 삭제", getApiErrorMessage(error)),
          });
        },
      },
    ]);
  };

  // 차단하면 친구 관계가 끊기고, 상대는 나를 검색하거나 친구 요청을 보낼 수 없다(알리지 않는다).
  const confirmBlock = () => {
    Alert.alert(
      "친구 차단",
      `${profile.nickname} 님을 차단할까요?\n친구 관계가 끊기고, 상대는 나를 검색하거나 친구 요청을 보낼 수 없어요. 차단은 설정 > 친구 설정에서 풀 수 있어요.`,
      [
        { text: "취소", style: "cancel" },
        {
          text: "차단",
          style: "destructive",
          onPress: () => {
            if (targetUserId === null) {
              return;
            }
            blockUserMutation.mutate(targetUserId, {
              onSuccess: () => router.back(),
              onError: (error) => Alert.alert("친구 차단", getApiErrorMessage(error)),
            });
          },
        },
      ],
    );
  };

  return (
    <View style={styles.container}>
      <BlurView
        intensity={28}
        tint="dark"
        style={[
          styles.titleArea,
          { minHeight: 60 + insets.top, paddingTop: insets.top },
        ]}
      >
        <View style={styles.headerOverlay} />
        <Pressable
          accessibilityLabel="뒤로 가기"
          accessibilityRole="button"
          hitSlop={10}
          onPress={() => router.back()}
          style={[styles.backButton, { top: 9 + insets.top }]}
        >
          <ChevronLeft color="#FFFFFF" size={28} strokeWidth={2.2} />
        </Pressable>
        <Text numberOfLines={1} style={styles.title}>
          {profile.nickname}
        </Text>
      </BlurView>

      <ScrollView
        bounces
        contentContainerStyle={[styles.scrollContent, { paddingTop: 60 + insets.top }]}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.profileArea}>
          <Pressable
            accessibilityLabel={`${profile.nickname}의 방 보기`}
            accessibilityRole="button"
            onPress={() => setIsRoomModalVisible(true)}
            style={styles.avatarButton}
          >
            <View>
              <AvatarFaceThumbnail
                backgroundColor={avatarColor.background}
                equipped={toEquippedMap(profile.avatarProfile?.equippedItems ?? [])}
                size={90}
              />
              {/* 접속 여부는 서버가 내려주지 않는다. 친구 목록과 같게 오늘 출석 여부로 표시한다. */}
              <View
                style={[
                  styles.attendanceIndicator,
                  profile.attendedToday ? styles.attended : styles.notAttended,
                ]}
              />
            </View>
            {/* 친구 방은 얼굴을 눌러야 열린다. 표시가 없으면 친구 방이 있는 줄 모르고 지나친다. */}
            <View style={styles.roomHint}>
              <House color="#FFFFFF" size={13} strokeWidth={2.4} />
              <Text style={styles.roomHintText}>방 보기</Text>
            </View>
          </Pressable>
          <Text style={styles.nickname}>{profile.nickname}</Text>
          <View style={styles.schoolRow}>
            <MapPin color="#E9E3FF" size={15} strokeWidth={2} />
            <Text style={styles.school}>
              {profile.schoolName ?? "학교 미등록"}
              {profile.schoolVerified ? " · 인증됨" : ""}
            </Text>
          </View>
          <View style={styles.summaryRow}>
            <View style={styles.summaryItem}>
              <Text style={styles.summaryValue}>Lv.{profile.level ?? 1}</Text>
              <Text style={styles.summaryLabel}>Level</Text>
            </View>
            <View style={styles.summaryDivider} />
            <View style={styles.summaryItem}>
              <View style={styles.streakValueRow}>
                <Flame color="#FDBA31" fill="#FDBA31" size={17} />
                <Text style={styles.summaryValue}>{profile.attendanceCount}</Text>
              </View>
              {/* 연속이 아니라 누적 출석 일수다. */}
              <Text style={styles.summaryLabel}>Total Days</Text>
            </View>
            <View style={styles.summaryDivider} />
            <View style={styles.summaryItem}>
              <Text style={styles.summaryValue}>{profile.attendedToday ? "O" : "X"}</Text>
              <Text style={styles.summaryLabel}>Today</Text>
            </View>
          </View>
        </View>

        <View style={styles.panel}>
          <View style={styles.panelContent}>
            {!isLearningPublic ? (
              <View style={styles.emptyStatsCard}>
                <Text style={styles.emptyStatsTitle}>Learning Stats</Text>
                <Text style={styles.emptyStatsText}>학습 기록을 공개하지 않은 친구예요</Text>
              </View>
            ) : progressAxes.length > 0 ? (
              <FriendProgressStats axes={progressAxes} />
            ) : (
              <View style={styles.emptyStatsCard}>
                <Text style={styles.emptyStatsTitle}>Learning Stats</Text>
                <Text style={styles.emptyStatsText}>아직 공개된 과목별 기록이 없어요</Text>
              </View>
            )}
            <View style={styles.attendanceCard}>
              <Text style={styles.attendanceTitle}>Attendance</Text>
              <Text style={styles.attendanceText}>
                누적 {profile.attendanceCount}일 ·{" "}
                {profile.attendedToday ? "오늘 출석 완료" : "오늘 아직 출석 전"}
              </Text>
              {profile.lastAttendanceDate ? (
                <Text style={styles.attendanceText}>
                  최근 출석 {profile.lastAttendanceDate.slice(0, 10).replace(/-/g, ".")}
                </Text>
              ) : null}
            </View>
            <View style={styles.learningCard}>
              <Text style={styles.learningTitle}>Learning History</Text>
              {isLearningPublic ? (
                <View style={styles.learningStats}>
                  <LearningStat
                    label="Study Time"
                    value={formatDuration(learningHistory?.totalMockSolveTimeSec ?? 0)}
                  />
                  <View style={styles.learningDivider} />
                  <LearningStat
                    label="Questions"
                    value={String(learningHistory?.conceptCorrectQuestionCount ?? 0)}
                  />
                  <View style={styles.learningDivider} />
                  <LearningStat
                    label="Completed"
                    value={String(learningHistory?.passedConceptCount ?? 0)}
                  />
                </View>
              ) : (
                <Text style={styles.emptyStatsText}>공개하지 않았어요</Text>
              )}
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityState={{ disabled: isPoked || isPokeBlocked }}
              disabled={isPoked || isPokeBlocked || pokeMutation.isPending}
              onPress={poke}
              style={[styles.pokeButton, isPoked && styles.pokeButtonActive]}
            >
              <Hand
                color={isPoked ? "#FFFFFF" : BRAND_PURPLE}
                fill={isPoked ? "#FFFFFF" : "transparent"}
                size={19}
                strokeWidth={2.1}
              />
              <Text style={[styles.pokeText, isPoked && styles.pokeTextActive]}>
                {isPoked ? "콕 찌름 완료" : "콕 찌르기"}
              </Text>
            </Pressable>
            {pokeMessage ? <Text style={styles.pokeMessage}>{pokeMessage}</Text> : null}
            <Pressable
              accessibilityRole="button"
              disabled={deleteFriendMutation.isPending}
              onPress={confirmDelete}
              style={styles.deleteButton}
            >
              <Text style={styles.deleteButtonText}>친구 삭제</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              disabled={blockUserMutation.isPending}
              hitSlop={8}
              onPress={confirmBlock}
              style={styles.blockButton}
            >
              <Text style={styles.blockButtonText}>차단하기</Text>
            </Pressable>
          </View>
        </View>
      </ScrollView>
      <FriendRoomModal
        nickname={profile.nickname}
        onClose={() => setIsRoomModalVisible(false)}
        room={profile.room}
        visible={isRoomModalVisible}
      />
    </View>
  );
}

/** 초 단위 누적 풀이 시간을 "36h 20m"으로. 1시간 미만이면 분만 보여준다. */
function formatDuration(seconds: number): string {
  const totalMinutes = Math.floor(seconds / 60);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;
}

function LearningStat({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.learningStat}>
      <Text style={styles.learningValue}>{value}</Text>
      <Text style={styles.learningLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: BRAND_PURPLE },
  centered: { alignItems: "center", justifyContent: "center", gap: 14 },
  errorText: {
    color: "#FFFFFF",
    fontFamily: FONT.pretendard,
    fontSize: 15,
  },
  errorAction: {
    color: "#E9E3FF",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
    textDecorationLine: "underline",
  },
  titleArea: {
    position: "absolute",
    top: 0,
    right: 0,
    left: 0,
    zIndex: 10,
    minHeight: 60,
    alignItems: "center",
    justifyContent: "center",
    overflow: "hidden",
  },
  headerOverlay: {
    ...StyleSheet.absoluteFill,
    backgroundColor: "rgba(109, 93, 251, 0.85)",
  },
  backButton: {
    position: "absolute",
    left: 10,
    width: 42,
    height: 42,
    alignItems: "center",
    justifyContent: "center",
    zIndex: 1,
  },
  title: {
    color: "#FFFFFF",
    fontFamily: FONT.pretendard,
    fontSize: 19,
    fontWeight: "700",
  },
  scrollContent: { backgroundColor: BRAND_PURPLE },
  profileArea: {
    alignItems: "center",
    gap: 8,
    paddingHorizontal: 24,
    paddingTop: 18,
    paddingBottom: 30,
  },
  avatarButton: {
    alignItems: "center",
    gap: 8,
  },
  attendanceIndicator: {
    position: "absolute",
    right: 1,
    bottom: 1,
    width: 18,
    height: 18,
    borderWidth: 3,
    borderColor: BRAND_PURPLE,
    borderRadius: 9,
  },
  attended: { backgroundColor: "#22C55E" },
  notAttended: { backgroundColor: "#94A3B8" },
  roomHint: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    backgroundColor: "rgba(255,255,255,0.2)",
  },
  roomHintText: {
    color: "#FFFFFF",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "600",
  },
  nickname: {
    marginTop: 4,
    color: "#FFFFFF",
    fontFamily: FONT.pretendard,
    fontSize: 22,
    fontWeight: "700",
  },
  schoolRow: { flexDirection: "row", alignItems: "center", gap: 4 },
  school: {
    color: "#F1EEFF",
    fontFamily: FONT.pretendard,
    fontSize: 13,
  },
  summaryRow: {
    width: "100%",
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-evenly",
    marginTop: 12,
  },
  summaryItem: { flex: 1, alignItems: "center", gap: 4 },
  summaryDivider: { width: 1, height: 30, backgroundColor: "rgba(255,255,255,0.32)" },
  summaryValue: {
    color: "#FFFFFF",
    fontFamily: FONT.kufam,
    fontSize: 15,
    fontWeight: "600",
  },
  streakValueRow: { flexDirection: "row", alignItems: "center", gap: 3 },
  summaryLabel: {
    color: "#E9E3FF",
    fontFamily: FONT.kufam,
    fontSize: 10,
  },
  panel: {
    width: "100%",
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: PANEL_GRAY,
    overflow: "hidden",
  },
  panelContent: {
    gap: 22,
    backgroundColor: PANEL_GRAY,
    paddingHorizontal: 24,
    paddingTop: 32,
    paddingBottom: 28 + OVERSCROLL_BUFFER,
  },
  emptyStatsCard: {
    borderRadius: 8,
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 18,
    paddingVertical: 16,
    gap: 5,
  },
  emptyStatsTitle: {
    color: "#1F2937",
    fontFamily: FONT.kufam,
    fontSize: 14,
    fontWeight: "600",
  },
  emptyStatsText: {
    color: "#94A3B8",
    fontFamily: FONT.pretendard,
    fontSize: 13,
  },
  attendanceCard: {
    borderRadius: 8,
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 18,
    paddingVertical: 16,
    gap: 5,
  },
  attendanceTitle: {
    color: "#1F2937",
    fontFamily: FONT.kufam,
    fontSize: 14,
    fontWeight: "600",
  },
  attendanceText: {
    color: "#64748B",
    fontFamily: FONT.pretendard,
    fontSize: 13,
  },
  learningCard: {
    borderRadius: 8,
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 18,
    paddingVertical: 16,
    gap: 15,
  },
  learningTitle: {
    color: "#1F2937",
    fontFamily: FONT.kufam,
    fontSize: 14,
    fontWeight: "600",
  },
  learningStats: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  learningStat: {
    flex: 1,
    alignItems: "center",
    gap: 5,
  },
  learningValue: {
    color: BRAND_PURPLE,
    fontFamily: FONT.kufam,
    fontSize: 14,
    fontWeight: "600",
  },
  learningLabel: {
    color: "#94A3B8",
    fontFamily: FONT.kufam,
    fontSize: 10,
  },
  learningDivider: {
    width: 1,
    height: 28,
    backgroundColor: "#E2E8F0",
  },
  pokeButton: {
    height: 50,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    borderWidth: 1,
    borderColor: BRAND_PURPLE,
    borderRadius: 8,
    backgroundColor: "#FFFFFF",
  },
  pokeButtonActive: { backgroundColor: BRAND_PURPLE },
  pokeText: {
    color: BRAND_PURPLE,
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "600",
  },
  pokeTextActive: { color: "#FFFFFF" },
  pokeMessage: {
    marginTop: -14,
    color: "#DC2626",
    fontFamily: FONT.pretendard,
    fontSize: 12,
  },
  deleteButton: {
    height: 48,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
    borderColor: "#FCA5A5",
    borderRadius: 8,
    backgroundColor: "#FFF7F7",
  },
  blockButton: {
    alignSelf: "center",
    paddingVertical: 10,
  },
  blockButtonText: {
    color: "#94A3B8",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
    textDecorationLine: "underline",
  },
  deleteButtonText: {
    color: "#DC2626",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
  },
});
