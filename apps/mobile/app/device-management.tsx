import { useState } from "react";
import { ActivityIndicator, Modal, Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../constants/fonts";
import {
  useRevokeOtherSessionsMutation,
  useRevokeSessionMutation,
  useSessionsQuery,
  type DeviceSession,
} from "../src/features/device";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

/** 해지 확인 모달이 무엇을 대상으로 하는지. null이면 닫힌 상태다. */
type PendingRevoke = { kind: "one"; session: DeviceSession } | { kind: "others" } | null;

export default function DeviceManagementScreen() {
  const { data: sessions, isPending, isError } = useSessionsQuery();
  const revokeSession = useRevokeSessionMutation();
  const revokeOthers = useRevokeOtherSessionsMutation();
  const [pending, setPending] = useState<PendingRevoke>(null);

  const isSubmitting = revokeSession.isPending || revokeOthers.isPending;
  const otherCount = (sessions ?? []).filter((session) => !session.current).length;

  const handleConfirm = () => {
    if (!pending || isSubmitting) {
      return;
    }
    const onSettled = () => setPending(null);

    if (pending.kind === "one") {
      revokeSession.mutate(pending.session.sessionId, { onSettled });
    } else {
      revokeOthers.mutate(undefined, { onSettled });
    }
  };

  return (
    <SettingsDetailLayout title="기기 관리">
      <Text style={detailStyles.sectionTitle}>로그인된 기기</Text>

      <View style={detailStyles.card}>
        {isPending ? (
          <ActivityIndicator color="#6D5DFB" style={styles.loading} />
        ) : isError ? (
          <Text style={detailStyles.bodyText}>
            기기 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.
          </Text>
        ) : (
          (sessions ?? []).map((session, index) => (
            <View
              key={session.sessionId}
              style={[styles.row, index > 0 && styles.rowDivider]}
            >
              <View style={styles.rowInfo}>
                <Text numberOfLines={1} style={styles.deviceName}>
                  {session.deviceName}
                </Text>
                <Text style={styles.deviceMeta}>{formatMeta(session)}</Text>
                <Text style={styles.deviceRegistered}>
                  {formatDate(session.createdAt)} 등록
                </Text>
              </View>

              {session.current ? (
                <View style={styles.currentBadge}>
                  <Text style={styles.currentBadgeText}>이 기기</Text>
                </View>
              ) : (
                <Pressable
                  style={styles.revokeButton}
                  disabled={isSubmitting}
                  onPress={() => setPending({ kind: "one", session })}
                >
                  <Text style={styles.revokeButtonText}>로그아웃</Text>
                </Pressable>
              )}
            </View>
          ))
        )}
      </View>

      <Text style={detailStyles.helperText}>
        기기 이름은 각 기기가 보내온 값이라 정확하지 않을 수 있습니다. 등록 시각을 함께
        확인해주세요. 접속 위치와 IP는 수집하지 않습니다.
      </Text>

      {otherCount > 0 && (
        <Pressable
          style={detailStyles.dangerButton}
          disabled={isSubmitting}
          onPress={() => setPending({ kind: "others" })}
        >
          <Text style={detailStyles.dangerButtonText}>다른 모든 기기에서 로그아웃</Text>
        </Pressable>
      )}

      <Modal
        animationType="fade"
        transparent
        visible={pending !== null}
        onRequestClose={() => setPending(null)}
      >
        <View style={styles.modalDim}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>
              {pending?.kind === "others"
                ? "다른 모든 기기에서 로그아웃할까요?"
                : "이 기기를 로그아웃할까요?"}
            </Text>
            <Text style={styles.modalDescription}>
              {pending?.kind === "others"
                ? `현재 기기를 제외한 ${otherCount}대가 즉시 로그아웃됩니다.`
                : `${pending?.kind === "one" ? pending.session.deviceName : ""}에서 즉시 로그아웃됩니다. 다시 사용하려면 그 기기에서 로그인해야 합니다.`}
            </Text>

            <View style={styles.modalActions}>
              <Pressable style={styles.cancelButton} onPress={() => setPending(null)}>
                <Text style={styles.cancelButtonText}>취소</Text>
              </Pressable>

              <Pressable
                style={styles.confirmButton}
                onPress={handleConfirm}
                disabled={isSubmitting}
              >
                <Text style={styles.confirmButtonText}>
                  {isSubmitting ? "처리 중…" : "로그아웃"}
                </Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </SettingsDetailLayout>
  );
}

function formatMeta(session: DeviceSession): string {
  const platform =
    session.platform === "IOS" ? "iOS" : session.platform === "ANDROID" ? "Android" : "기타";
  const os = session.osVersion ? `${platform} ${session.osVersion}` : platform;
  return `${os} · ${formatRelative(session.lastSeenAt)}`;
}

/** 서버는 타임존 없는 LocalDateTime을 준다. 기기 로컬 시각으로 해석한다. */
function parse(raw: string): Date {
  return new Date(raw);
}

function formatRelative(raw: string): string {
  const diffMs = Date.now() - parse(raw).getTime();
  const minutes = Math.floor(diffMs / 60000);

  if (minutes < 1) {
    return "방금 전";
  }
  if (minutes < 60) {
    return `${minutes}분 전`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `${hours}시간 전`;
  }
  return `${Math.floor(hours / 24)}일 전`;
}

function formatDate(raw: string): string {
  const date = parse(raw);
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, "0")}.${String(
    date.getDate(),
  ).padStart(2, "0")}`;
}

const styles = StyleSheet.create({
  loading: {
    paddingVertical: 24,
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 12,
    paddingVertical: 12,
  },
  rowDivider: {
    borderTopWidth: 1,
    borderTopColor: "#EEF2F7",
  },
  rowInfo: {
    flex: 1,
  },
  deviceName: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
  deviceMeta: {
    marginTop: 3,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 13,
  },
  deviceRegistered: {
    marginTop: 2,
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
  },
  currentBadge: {
    borderRadius: 999,
    backgroundColor: "#EEF2FF",
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  currentBadgeText: {
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "700",
  },
  revokeButton: {
    borderRadius: 12,
    backgroundColor: "#F1F5F9",
    paddingHorizontal: 12,
    paddingVertical: 9,
  },
  revokeButtonText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
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
  confirmButton: {
    flex: 1,
    minHeight: 48,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 15,
    backgroundColor: "#6D5DFB",
  },
  confirmButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
});
