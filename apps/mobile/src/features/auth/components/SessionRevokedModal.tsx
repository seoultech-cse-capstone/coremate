import { router } from "expo-router";
import { Modal, Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../../constants/fonts";
import type { SessionRevokedCode } from "../../../shared/api/httpClient";
import { useAuth } from "../AuthContext";

const MESSAGES: Record<SessionRevokedCode, { title: string; description: string }> = {
  SESSION_REVOKED_BY_OTHER_DEVICE: {
    title: "다른 기기에서 로그인했어요",
    description:
      "다른 기기에서 로그인하여 이 기기에서는 로그아웃되었습니다. 계속 사용하려면 다시 로그인해주세요.",
  },
  SESSION_REVOKED_BY_USER: {
    title: "이 기기의 로그인이 해지되었어요",
    description:
      "기기 관리에서 이 기기의 로그인이 해지되었습니다. 계속 사용하려면 다시 로그인해주세요.",
  },
};

/**
 * 기기 세션이 해지되었을 때 뜨는 전역 안내.
 *
 * ⚠️ 반드시 Stack 바깥(app/_layout.tsx)에 두어야 한다. 화면 컴포넌트 안에 두면
 * status가 바뀌면서 탭 네비게이터가 통째로 재마운트되고 로그인 화면으로 리다이렉트되어,
 * 모달이 뜨기 전에 언마운트된다.
 *
 * 본인 로그아웃·비밀번호 재설정·회원탈퇴로 인한 해지는 사용자가 이미 이유를 알기 때문에
 * 여기서 다루지 않는다(그때는 revokedReason이 null이다).
 */
export function SessionRevokedModal() {
  const { revokedReason, clearRevokedReason } = useAuth();

  if (!revokedReason) {
    return null;
  }

  const { title, description } = MESSAGES[revokedReason];

  const handleConfirm = () => {
    clearRevokedReason();
    router.replace("/");
  };

  return (
    <Modal animationType="fade" transparent visible onRequestClose={handleConfirm}>
      <View style={styles.modalDim}>
        <View style={styles.modalCard}>
          <Text style={styles.modalTitle}>{title}</Text>
          <Text style={styles.modalDescription}>{description}</Text>
          <Text style={styles.modalNotice}>
            진행 중이던 모의고사가 있었다면 해당 회차는 중단 처리됩니다.
          </Text>

          <Pressable style={styles.confirmButton} onPress={handleConfirm}>
            <Text style={styles.confirmButtonText}>확인</Text>
          </Pressable>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
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
  modalNotice: {
    marginTop: 10,
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    lineHeight: 18,
  },
  confirmButton: {
    minHeight: 48,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 15,
    backgroundColor: "#6D5DFB",
    marginTop: 20,
  },
  confirmButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
});
