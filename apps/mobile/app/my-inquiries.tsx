import { ActivityIndicator, StyleSheet, Text, View } from "react-native";

import { useAuth, useRequireMember } from "../src/features/auth";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";
import {
  type Inquiry,
  type InquiryCategory,
} from "../src/features/support/supportApi";
import { useMyInquiries } from "../src/features/support/useInquiryQueries";

// 문의 작성 화면과 같은 카테고리 라벨을 쓴다.
const CATEGORY_LABEL: Record<InquiryCategory, string> = {
  HOME: "홈",
  FRIEND: "친구",
  PROBLEM_SET: "문제집",
  REVIEW: "복습",
  MYPAGE: "마이페이지",
};

// "2026-10-26T19:10:11" → "2026.10.26"
function formatDate(iso: string): string {
  return iso.slice(0, 10).replace(/-/g, ".");
}

function InquiryItem({ inquiry }: { inquiry: Inquiry }) {
  const answered = inquiry.status === "ANSWERED";
  return (
    <View style={detailStyles.card}>
      <View style={styles.cardHeader}>
        <View style={styles.categoryChip}>
          <Text style={styles.categoryChipText}>
            {CATEGORY_LABEL[inquiry.category]}
          </Text>
        </View>
        <View style={[styles.statusChip, answered && styles.statusChipAnswered]}>
          <Text
            style={[styles.statusText, answered && styles.statusTextAnswered]}
          >
            {answered ? "답변 완료" : "답변 대기"}
          </Text>
        </View>
      </View>

      <Text style={styles.title}>{inquiry.title}</Text>
      <Text style={styles.date}>{formatDate(inquiry.createdAt)}</Text>
      <Text style={styles.body}>{inquiry.body}</Text>

      {answered && inquiry.answer ? (
        <View style={styles.answerBox}>
          <Text style={styles.answerLabel}>운영자 답변</Text>
          <Text style={styles.answerBody}>{inquiry.answer}</Text>
          {inquiry.answeredAt ? (
            <Text style={styles.answerDate}>
              {formatDate(inquiry.answeredAt)}
            </Text>
          ) : null}
        </View>
      ) : null}
    </View>
  );
}

export default function MyInquiriesScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const { data: inquiries, isPending, isError } = useMyInquiries(accessToken);

  return (
    <SettingsDetailLayout title="내 문의 내역">
      {isPending ? (
        <ActivityIndicator color="#6D5DFB" style={styles.center} />
      ) : isError ? (
        <View style={detailStyles.card}>
          <Text style={detailStyles.bodyText}>
            문의 내역을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
          </Text>
        </View>
      ) : inquiries.length === 0 ? (
        <View style={detailStyles.card}>
          <Text style={detailStyles.bodyText}>아직 보낸 문의가 없습니다.</Text>
        </View>
      ) : (
        inquiries.map((inquiry) => (
          <InquiryItem key={inquiry.inquiryId} inquiry={inquiry} />
        ))
      )}
    </SettingsDetailLayout>
  );
}

const styles = StyleSheet.create({
  center: {
    marginTop: 40,
  },
  cardHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 10,
  },
  categoryChip: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    backgroundColor: "rgba(109, 93, 251, 0.12)",
  },
  categoryChipText: {
    color: "#6D5DFB",
    fontSize: 12,
    fontWeight: "700",
  },
  statusChip: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    backgroundColor: "#F1F5F9",
  },
  statusChipAnswered: {
    backgroundColor: "rgba(34, 197, 94, 0.12)",
  },
  statusText: {
    color: "#64748b",
    fontSize: 12,
    fontWeight: "700",
  },
  statusTextAnswered: {
    color: "#16a34a",
  },
  title: {
    color: "#111827",
    fontSize: 16,
    fontWeight: "700",
  },
  date: {
    marginTop: 4,
    color: "#94a3b8",
    fontSize: 12,
  },
  body: {
    marginTop: 10,
    color: "#475569",
    fontSize: 14,
    lineHeight: 22,
  },
  answerBox: {
    marginTop: 14,
    padding: 14,
    borderRadius: 14,
    backgroundColor: "#ffffff",
    borderWidth: 1,
    borderColor: "#E5E7EB",
  },
  answerLabel: {
    color: "#6D5DFB",
    fontSize: 13,
    fontWeight: "700",
  },
  answerBody: {
    marginTop: 8,
    color: "#334155",
    fontSize: 14,
    lineHeight: 22,
  },
  answerDate: {
    marginTop: 8,
    textAlign: "right",
    color: "#94a3b8",
    fontSize: 12,
  },
});
