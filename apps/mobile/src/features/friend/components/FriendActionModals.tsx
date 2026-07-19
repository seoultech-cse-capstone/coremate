import { Search, X } from "lucide-react-native";
import { Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";

import { FONT } from "../../../../constants/fonts";
import type { Friend } from "../friendData";
import { FriendAvatar } from "./FriendAvatar";
import { FriendRequestCard } from "./FriendRequestCard";

type ModalProps = {
  visible: boolean;
  query: string;
  friends: Friend[];
  onClose: () => void;
  onQueryChange: (query: string) => void;
};

type FriendSearchModalProps = ModalProps & { onOpenProfile: (friendId: string) => void };

export function FriendSearchModal({ visible, query, friends, onClose, onQueryChange, onOpenProfile }: FriendSearchModalProps) {
  return (
    <Modal animationType="fade" onRequestClose={onClose} transparent visible={visible}>
      <ModalSurface onClose={onClose} onQueryChange={onQueryChange} query={query} title="Friend Search">
        {friends.map((friend) => (
          <Pressable key={friend.id} onPress={() => onOpenProfile(friend.id)} style={styles.row}>
            <FriendAvatar backgroundColor={friend.avatarColor} color={friend.avatarAccentColor} />
            <FriendInfo friend={friend} />
          </Pressable>
        ))}
        {friends.length === 0 && <Text style={styles.empty}>No results found.</Text>}
      </ModalSurface>
    </Modal>
  );
}

type AddFriendModalProps = ModalProps & {
  sentRequestIds: string[];
  isRequestLimitReached: boolean;
  onSendRequest: (friendId: string) => void;
};

export function AddFriendModal({ visible, query, friends, onClose, onQueryChange, sentRequestIds, isRequestLimitReached, onSendRequest }: AddFriendModalProps) {
  return (
    <Modal animationType="fade" onRequestClose={onClose} transparent visible={visible}>
      <ModalSurface onClose={onClose} onQueryChange={onQueryChange} query={query} title="Add Friend">
        {friends.map((friend) => {
          const isSent = sentRequestIds.includes(friend.id);
          return (
            <View key={friend.id} style={styles.row}>
              <FriendAvatar backgroundColor={friend.avatarColor} color={friend.avatarAccentColor} />
              <FriendInfo friend={friend} />
              <Pressable disabled={isSent || isRequestLimitReached} onPress={() => onSendRequest(friend.id)} style={[styles.requestButton, (isSent || isRequestLimitReached) && styles.requestButtonSent]}>
                <Text style={[styles.requestText, (isSent || isRequestLimitReached) && styles.requestTextSent]}>{isSent ? "Sent" : "Request"}</Text>
              </Pressable>
            </View>
          );
        })}
        {friends.length === 0 && <Text style={styles.empty}>No results found.</Text>}
        {isRequestLimitReached && <Text style={styles.limitText}>You can send up to 30 friend requests.</Text>}
      </ModalSurface>
    </Modal>
  );
}

type ReceivedRequestsModalProps = {
  visible: boolean;
  friends: Friend[];
  onClose: () => void;
  onAccept: (friend: Friend) => void;
  onDecline: (friendId: string) => void;
};

export function ReceivedRequestsModal({ visible, friends, onClose, onAccept, onDecline }: ReceivedRequestsModalProps) {
  return (
    <Modal animationType="fade" onRequestClose={onClose} transparent visible={visible}>
      <View style={styles.overlay}>
        <View style={styles.modal}>
          <View style={styles.header}>
            <Text style={styles.title}>Friend Requests</Text>
            <Pressable accessibilityLabel="Close" hitSlop={8} onPress={onClose}><X color="#475569" size={22} /></Pressable>
          </View>
          <ScrollView contentContainerStyle={styles.list} showsVerticalScrollIndicator={false}>
            {friends.map((friend) => (
              <FriendRequestCard
                friend={friend}
                key={friend.id}
                onAccept={() => onAccept(friend)}
                onDecline={() => onDecline(friend.id)}
              />
            ))}
            {friends.length === 0 && <Text style={styles.empty}>No pending requests.</Text>}
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
}: Pick<ModalProps, "query" | "onClose" | "onQueryChange"> & {
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
          <TextInput autoFocus onChangeText={onQueryChange} placeholder="Search by nickname" placeholderTextColor="#94A3B8" style={styles.input} value={query} />
        </View>
        <ScrollView contentContainerStyle={styles.list} showsVerticalScrollIndicator={false}>{children}</ScrollView>
      </View>
    </View>
  );
}

function FriendInfo({ friend }: { friend: Friend }) {
  return (
    <View style={styles.info}>
      <Text style={styles.name}>{friend.nickname}</Text>
      <Text numberOfLines={1} style={styles.detail}>{friend.school} · {friend.department}</Text>
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
