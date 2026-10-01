import { Search, X } from "lucide-react-native";
import { Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";

import { FONT } from "../../../../constants/fonts";
import { AvatarFaceThumbnail, toEquippedMap, type AvatarEquippedItem } from "../../avatar";
import { getAvatarColor } from "../avatarColor";
import type { Friend, FriendRequest, FriendSearchUser } from "../types";
import { FriendRequestCard } from "./FriendRequestCard";

type ModalShellProps = {
  visible: boolean;
  query: string;
  onClose: () => void;
  onQueryChange: (query: string) => void;
};

type FriendSearchModalProps = ModalShellProps & {
  friends: Friend[];
  onOpenProfile: (friendUserId: number) => void;
};

export function FriendSearchModal({ visible, query, friends, onClose, onQueryChange, onOpenProfile }: FriendSearchModalProps) {
  return (
    <Modal animationType="fade" onRequestClose={onClose} transparent visible={visible}>
      <ModalSurface onClose={onClose} onQueryChange={onQueryChange} query={query} title="Friend Search">
        {friends.map((friend) => (
          <Pressable
            key={friend.friendUserId}
            onPress={() => onOpenProfile(friend.friendUserId)}
            style={styles.row}
          >
            <PersonAvatar equippedItems={friend.avatarProfile?.equippedItems ?? []} nickname={friend.nickname} />
            <PersonInfo nickname={friend.nickname} schoolName={friend.schoolName} />
          </Pressable>
        ))}
        {friends.length === 0 && (
          <Text style={styles.empty}>
            {query.trim() ? "검색 결과가 없어요." : "닉네임을 입력해 친구를 찾아보세요."}
          </Text>
        )}
      </ModalSurface>
    </Modal>
  );
}

type AddFriendModalProps = ModalShellProps & {
  users: FriendSearchUser[];
  /** 이번 화면에서 방금 요청을 보낸 사용자. 서버 응답이 갱신되기 전에도 버튼을 눌린 상태로 둔다. */
  sentUserIds: number[];
  isRequestLimitReached: boolean;
  onSendRequest: (userId: number) => void;
};

export function AddFriendModal({ visible, query, users, onClose, onQueryChange, sentUserIds, isRequestLimitReached, onSendRequest }: AddFriendModalProps) {
  return (
    <Modal animationType="fade" onRequestClose={onClose} transparent visible={visible}>
      <ModalSurface onClose={onClose} onQueryChange={onQueryChange} query={query} title="Add Friend">
        {users.map((user) => {
          const button = requestButton(user, sentUserIds.includes(user.userId));
          const disabled = !button.canRequest || isRequestLimitReached;
          return (
            <View key={user.userId} style={styles.row}>
              <PersonAvatar equippedItems={user.equippedItems ?? []} nickname={user.nickname} />
              <PersonInfo nickname={user.nickname} schoolName={user.schoolName} />
              <Pressable disabled={disabled} onPress={() => onSendRequest(user.userId)} style={[styles.requestButton, disabled && styles.requestButtonSent]}>
                <Text style={[styles.requestText, disabled && styles.requestTextSent]}>{button.label}</Text>
              </Pressable>
            </View>
          );
        })}
        {users.length === 0 && (
          <Text style={styles.empty}>
            {query.trim() ? "검색 결과가 없어요." : "닉네임을 입력해 친구를 찾아보세요."}
          </Text>
        )}
        {isRequestLimitReached && <Text style={styles.limitText}>친구 요청은 최대 30명까지 보낼 수 있어요.</Text>}
      </ModalSurface>
    </Modal>
  );
}

/**
 * 검색 결과의 친구 요청 버튼. 관계 상태는 서버가 검색한 사람 기준으로 내려준다 — 내가 보낸 요청이
 * 거절됐어도 REQUESTED 로 와서 계속 "요청함"으로 보인다(거절은 알리지 않는다).
 */
function requestButton(user: FriendSearchUser, justSent: boolean): { label: string; canRequest: boolean } {
  if (justSent) {
    return { label: "요청함", canRequest: false };
  }
  switch (user.relationshipStatus) {
    case null:
    // 내가 거절했던 사람. 마음을 바꾸면 이쪽에서 먼저 요청할 수 있다.
    case "REJECTED":
      return { label: "친구 요청", canRequest: true };
    case "ACCEPTED":
      return { label: "친구", canRequest: false };
    case "REQUESTED":
      // 상대가 보낸 요청은 "요청 온 친구"에서 수락한다.
      return { label: user.direction === "RECEIVED" ? "요청 받음" : "요청함", canRequest: false };
    default:
      return { label: "요청함", canRequest: false };
  }
}

type ReceivedRequestsModalProps = {
  visible: boolean;
  requests: FriendRequest[];
  onClose: () => void;
  onAccept: (friendId: number) => void;
  onDecline: (friendId: number) => void;
};

export function ReceivedRequestsModal({ visible, requests, onClose, onAccept, onDecline }: ReceivedRequestsModalProps) {
  return (
    <Modal animationType="fade" onRequestClose={onClose} transparent visible={visible}>
      <View style={styles.overlay}>
        <View style={styles.modal}>
          <View style={styles.header}>
            <Text style={styles.title}>Friend Requests</Text>
            <Pressable accessibilityLabel="Close" hitSlop={8} onPress={onClose}><X color="#475569" size={22} /></Pressable>
          </View>
          <ScrollView contentContainerStyle={styles.list} showsVerticalScrollIndicator={false}>
            {requests.map((request) => (
              <FriendRequestCard
                key={request.friendId}
                equippedItems={request.requesterEquippedItems ?? []}
                nickname={request.requesterNickname}
                schoolName={null}
                onAccept={() => onAccept(request.friendId)}
                onDecline={() => onDecline(request.friendId)}
              />
            ))}
            {requests.length === 0 && <Text style={styles.empty}>받은 친구 요청이 없어요.</Text>}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

function ModalSurface({
  title,
  query,
  onClose,
  onQueryChange,
  children,
}: Pick<ModalShellProps, "query" | "onClose" | "onQueryChange"> & {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <View style={styles.overlay}>
      <View style={styles.modal}>
        <View style={styles.header}>
          <Text style={styles.title}>{title}</Text>
          <Pressable accessibilityLabel="Close" hitSlop={8} onPress={onClose}><X color="#475569" size={22} /></Pressable>
        </View>
        <View style={styles.inputWrap}>
          <Search color="#94A3B8" size={18} />
          <TextInput autoFocus onChangeText={onQueryChange} placeholder="닉네임으로 검색" placeholderTextColor="#94A3B8" style={styles.input} value={query} />
        </View>
        <ScrollView contentContainerStyle={styles.list} showsVerticalScrollIndicator={false}>{children}</ScrollView>
      </View>
    </View>
  );
}

function PersonAvatar({ nickname, equippedItems }: { nickname: string; equippedItems: AvatarEquippedItem[] }) {
  const color = getAvatarColor(nickname);
  return <AvatarFaceThumbnail backgroundColor={color.background} equipped={toEquippedMap(equippedItems)} size={44} />;
}

function PersonInfo({ nickname, schoolName }: { nickname: string; schoolName: string | null }) {
  return (
    <View style={styles.info}>
      <Text style={styles.name}>{nickname}</Text>
      {schoolName ? (
        <Text numberOfLines={1} style={styles.detail}>{schoolName}</Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: { flex: 1, justifyContent: "center", backgroundColor: "rgba(15, 23, 42, 0.42)", paddingHorizontal: 24 },
  modal: { maxHeight: "72%", borderRadius: 8, backgroundColor: "#FFFFFF", padding: 20 },
  header: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  title: { color: "#1F2937", fontFamily: FONT.kufam, fontSize: 18, fontWeight: "600" },
  inputWrap: { height: 46, flexDirection: "row", alignItems: "center", gap: 8, marginTop: 18, borderRadius: 8, backgroundColor: "#F1F5F9", paddingHorizontal: 13 },
  input: { flex: 1, color: "#1F2937", fontFamily: FONT.pretendard, fontSize: 14, padding: 0 },
  list: { gap: 10, paddingTop: 16 },
  row: { minHeight: 62, flexDirection: "row", alignItems: "center", gap: 10, borderBottomWidth: 1, borderBottomColor: "#EEF2F7", paddingVertical: 8 },
  info: { flex: 1, gap: 3 },
  name: { color: "#1F2937", fontFamily: FONT.pretendard, fontSize: 14, fontWeight: "600" },
  detail: { color: "#94A3B8", fontFamily: FONT.pretendard, fontSize: 11 },
  requestButton: { height: 32, alignItems: "center", justifyContent: "center", borderRadius: 8, backgroundColor: "#6D5DFB", paddingHorizontal: 12 },
  requestButtonSent: { backgroundColor: "#E9E3FF" },
  requestText: { color: "#FFFFFF", fontFamily: FONT.pretendard, fontSize: 12, fontWeight: "600" },
  requestTextSent: { color: "#6D5DFB" },
  empty: { color: "#94A3B8", fontFamily: FONT.pretendard, fontSize: 14, textAlign: "center", paddingVertical: 28 },
  limitText: { color: "#DC2626", fontFamily: FONT.pretendard, fontSize: 12, textAlign: "center", paddingTop: 8 },
});
