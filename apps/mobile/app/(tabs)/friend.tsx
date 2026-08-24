import { BlurView } from "expo-blur";
import { router, useNavigation } from "expo-router";
import { Search, UserPlus } from "lucide-react-native";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../../constants/fonts";
import { useRequireMember } from "../../src/features/auth";
import {
  AddFriendModal,
  FriendSearchModal,
  ReceivedRequestsModal,
} from "../../src/features/friend/components/FriendActionModals";
import { FriendListCard } from "../../src/features/friend/components/FriendListCard";
import {
  isPokeLimitError,
  usePokeMutation,
  useFriendsQuery,
  useReceivedRequestsQuery,
  useRespondRequestMutation,
  useSendFriendRequestMutation,
  useUserSearchQuery,
} from "../../src/features/friend";
import type { Friend } from "../../src/features/friend/types";
import { getApiErrorMessage } from "../../src/shared/utils/apiError";
import { refetchActiveQueries } from "../../src/shared/api/queryClient";

const BRAND_PURPLE = "#6D5DFB";
const PANEL_GRAY = "#F3F3F3";
const OVERSCROLL_BUFFER = 96;

type TabPressNavigation = {
  addListener: (eventName: "tabPress", callback: () => void) => () => void;
};

export default function FriendScreen() {
  useRequireMember();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const scrollRef = useRef<ScrollView>(null);
  const { height } = useWindowDimensions();
  const [bounceBackground, setBounceBackground] = useState(BRAND_PURPLE);
  const [pokedFriendIds, setPokedFriendIds] = useState<number[]>([]);
  // 서버가 한도 초과로 막은 대상. 다음 날까지 버튼을 비활성으로 둔다.
  const [pokeBlockedIds, setPokeBlockedIds] = useState<number[]>([]);
  const [pokeMessage, setPokeMessage] = useState<string | null>(null);
  const [isSearchModalVisible, setIsSearchModalVisible] = useState(false);
  const [isAddModalVisible, setIsAddModalVisible] = useState(false);
  const [isReceivedModalVisible, setIsReceivedModalVisible] = useState(false);
  const [friendQuery, setFriendQuery] = useState("");
  const [addQuery, setAddQuery] = useState("");
  const [sentUserIds, setSentUserIds] = useState<number[]>([]);

  const { data: friends = [] } = useFriendsQuery();
  const { data: receivedRequests = [] } = useReceivedRequestsQuery();
  const { data: searchResults = [] } = useUserSearchQuery(addQuery);
  const sendRequestMutation = useSendFriendRequestMutation();
  const respondRequestMutation = useRespondRequestMutation();
  const pokeMutation = usePokeMutation();

  const panelMinHeight = Math.max(0, height - (60 + insets.top));

  // 온라인 여부는 서버가 내려주지 않고 요구사항에도 없다. SCR-FRIEND-01 이 정의한
  // "오늘 출석 여부"로 두 묶음을 나눈다.
  const attendedFriends = useMemo(() => friends.filter((f) => f.attendedToday), [friends]);
  const notAttendedFriends = useMemo(() => friends.filter((f) => !f.attendedToday), [friends]);
  const searchedFriends = useMemo(
    () =>
      friends.filter((friend) =>
        friend.nickname.toLowerCase().includes(friendQuery.trim().toLowerCase()),
      ),
    [friends, friendQuery],
  );

  useEffect(() => {
    const tabNavigation = navigation as unknown as TabPressNavigation;
    const unsubscribe = tabNavigation.addListener("tabPress", () => {
      scrollRef.current?.scrollTo({ y: 0, animated: true });
      refetchActiveQueries();
    });

    return unsubscribe;
  }, [navigation]);

  const handleScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
    const { contentOffset, contentSize, layoutMeasurement } = event.nativeEvent;
    const maxScrollY = Math.max(
      0,
      contentSize.height - layoutMeasurement.height,
    );
    const isNearBottom =
      maxScrollY > 0 && contentOffset.y > maxScrollY - OVERSCROLL_BUFFER;
    const nextBackground = isNearBottom ? PANEL_GRAY : BRAND_PURPLE;

    setBounceBackground((current) =>
      current === nextBackground ? current : nextBackground,
    );
  };

  // 콕 찌르기는 취소가 없다. 서버에 기록이 남는 행위라 다시 눌러도 되돌릴 수 없다.
  const poke = (friendUserId: number) => {
    if (pokedFriendIds.includes(friendUserId) || pokeBlockedIds.includes(friendUserId)) {
      return;
    }
    pokeMutation.mutate(friendUserId, {
      onSuccess: () => {
        setPokedFriendIds((current) => [...current, friendUserId]);
        setPokeMessage(null);
      },
      onError: (error) => {
        if (isPokeLimitError(error)) {
          setPokeBlockedIds((current) => [...current, friendUserId]);
        }
        setPokeMessage(getApiErrorMessage(error));
      },
    });
  };

  const openProfile = (friendUserId: number) =>
    router.push({ pathname: "/friend-profile", params: { friendId: String(friendUserId) } });

  const acceptRequest = (friendId: number) => {
    respondRequestMutation.mutate({ friendId, status: "ACCEPTED" });
  };

  const declineRequest = (friendId: number) => {
    respondRequestMutation.mutate({ friendId, status: "REJECTED" });
  };

  return (
    <View style={[styles.container, { backgroundColor: bounceBackground }]}>
      <BlurView
        intensity={28}
        tint="dark"
        style={[
          styles.titleArea,
          { minHeight: 60 + insets.top, paddingTop: insets.top },
        ]}
      >
        <View style={styles.headerOverlay} />
        <Text style={styles.title}>Friends</Text>
        <Pressable
          accessibilityLabel="Search friends"
          accessibilityRole="button"
          hitSlop={10}
          onPress={() => setIsSearchModalVisible(true)}
          style={[styles.searchButton, { top: 9 + insets.top }]}
        >
          <Search color="#FFFFFF" size={22} strokeWidth={2.1} />
        </Pressable>
        <Pressable
          accessibilityLabel="친구 추가"
          accessibilityRole="button"
          hitSlop={10}
          onPress={() => setIsAddModalVisible(true)}
          style={[styles.addButton, { top: 9 + insets.top }]}
        >
          <UserPlus color="#FFFFFF" size={22} strokeWidth={2.1} />
        </Pressable>
      </BlurView>

      <ScrollView
        bounces
        contentContainerStyle={[
          styles.scrollContent,
          { paddingTop: 60 + insets.top },
        ]}
        onScroll={handleScroll}
        ref={scrollRef}
        scrollEventThrottle={16}
        showsVerticalScrollIndicator={false}
        style={[styles.scroller, { backgroundColor: bounceBackground }]}
      >
        <View style={styles.purpleSpacer} />
        <View style={[styles.panel, { minHeight: panelMinHeight }]}>
          <View style={styles.panelContent}>
            <Pressable
              accessibilityRole="button"
              onPress={() => setIsReceivedModalVisible(true)}
              style={styles.receivedButton}
            >
              <View style={[styles.statusDot, styles.requestDot]} />
              <Text style={styles.receivedButtonText}>요청 온 친구</Text>
              <View style={styles.requestCount}>
                <Text style={styles.requestCountText}>{receivedRequests.length}</Text>
              </View>
            </Pressable>
            {pokeMessage ? <Text style={styles.pokeMessage}>{pokeMessage}</Text> : null}
            {friends.length === 0 ? (
              <Text style={styles.emptyText}>
                아직 친구가 없어요. 오른쪽 위에서 닉네임으로 친구를 찾아보세요.
              </Text>
            ) : null}
            <FriendSection
              friends={attendedFriends}
              isPoked={(id) => pokedFriendIds.includes(id)}
              isPokeDisabled={(id) => pokeBlockedIds.includes(id)}
              onOpenProfile={openProfile}
              onPoke={poke}
              title="오늘 출석"
              statusColor="#22C55E"
            />
            <FriendSection
              friends={notAttendedFriends}
              isPoked={(id) => pokedFriendIds.includes(id)}
              isPokeDisabled={(id) => pokeBlockedIds.includes(id)}
              onOpenProfile={openProfile}
              onPoke={poke}
              title="아직 출석 전"
              statusColor="#94A3B8"
            />
          </View>
        </View>
      </ScrollView>
      <FriendSearchModal
        friends={searchedFriends}
        onClose={() => {
          setIsSearchModalVisible(false);
          setFriendQuery("");
        }}
        onOpenProfile={(friendUserId) => {
          setIsSearchModalVisible(false);
          setFriendQuery("");
          openProfile(friendUserId);
        }}
        onQueryChange={setFriendQuery}
        query={friendQuery}
        visible={isSearchModalVisible}
      />
      <AddFriendModal
        users={searchResults}
        onClose={() => {
          setIsAddModalVisible(false);
          setAddQuery("");
          setSentUserIds([]);
        }}
        onQueryChange={setAddQuery}
        isRequestLimitReached={false}
        onSendRequest={(userId) => {
          sendRequestMutation.mutate(userId, {
            onSuccess: () => setSentUserIds((current) => [...current, userId]),
          });
        }}
        query={addQuery}
        sentUserIds={sentUserIds}
        visible={isAddModalVisible}
      />
      <ReceivedRequestsModal
        requests={receivedRequests}
        onAccept={acceptRequest}
        onClose={() => setIsReceivedModalVisible(false)}
        onDecline={declineRequest}
        visible={isReceivedModalVisible}
      />
    </View>
  );
}

type FriendSectionProps = {
  friends: Friend[];
  title: string;
  statusColor: string;
  isPoked: (friendUserId: number) => boolean;
  isPokeDisabled: (friendUserId: number) => boolean;
  onOpenProfile: (friendUserId: number) => void;
  onPoke: (friendUserId: number) => void;
};

function FriendSection({
  friends,
  title,
  statusColor,
  isPoked,
  isPokeDisabled,
  onOpenProfile,
  onPoke,
}: FriendSectionProps) {
  if (friends.length === 0) {
    return null;
  }

  return (
    <View style={styles.section}>
      <View style={styles.sectionTitleRow}>
        <View style={[styles.statusDot, { backgroundColor: statusColor }]} />
        <Text style={styles.sectionTitle}>{title}</Text>
      </View>
      <View style={styles.cardList}>
        {friends.map((friend) => (
          <FriendListCard
            friend={friend}
            isPoked={isPoked(friend.friendUserId)}
            isPokeDisabled={isPokeDisabled(friend.friendUserId)}
            key={friend.friendUserId}
            onPoke={() => onPoke(friend.friendUserId)}
            onPress={() => onOpenProfile(friend.friendUserId)}
          />
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  pokeMessage: {
    color: "#DC2626",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    paddingBottom: 8,
  },
  emptyText: {
    marginTop: 24,
    color: "#94A3B8",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    lineHeight: 21,
    textAlign: "center",
  },
  container: { flex: 1 },
  titleArea: {
    position: "absolute",
    top: 0,
    right: 0,
    left: 0,
    zIndex: 10,
    minHeight: 60,
    flexDirection: "row",
    alignItems: "center",
    overflow: "hidden",
    paddingHorizontal: 16,
  },
  headerOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(109, 93, 251, 0.85)",
  },
  title: {
    color: "#FFFFFF",
    fontFamily: FONT.pretendard,
    fontSize: 24,
    fontWeight: "700",
  },
  addButton: {
    position: "absolute",
    right: 16,
    width: 42,
    height: 42,
    alignItems: "center",
    justifyContent: "center",
  },
  searchButton: {
    position: "absolute",
    right: 56,
    width: 42,
    height: 42,
    alignItems: "center",
    justifyContent: "center",
  },
  scroller: { flex: 1 },
  scrollContent: { backgroundColor: BRAND_PURPLE },
  purpleSpacer: { height: 12 },
  panel: {
    width: "100%",
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: PANEL_GRAY,
    overflow: "hidden",
  },
  panelContent: {
    gap: 34,
    backgroundColor: PANEL_GRAY,
    paddingHorizontal: 24,
    paddingTop: 28,
    paddingBottom: 28 + OVERSCROLL_BUFFER,
  },
  section: { gap: 12 },
  sectionTitleRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 7,
  },
  statusDot: { width: 6, height: 6, borderRadius: 3 },
  sectionTitle: {
    color: "#475569",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "500",
  },
  cardList: { gap: 10 },
  receivedButton: {
    height: 46,
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    borderRadius: 8,
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 14,
    shadowColor: "#64748B",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.07,
    shadowRadius: 5,
    elevation: 2,
  },
  receivedButtonText: {
    flex: 1,
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
  },
  requestDot: { backgroundColor: "#F59E0B" },
  requestCount: {
    minWidth: 18,
    height: 18,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 9,
    backgroundColor: "#FFF0D2",
    paddingHorizontal: 5,
  },
  requestCountText: {
    color: "#B45309",
    fontFamily: FONT.kufam,
    fontSize: 10,
    fontWeight: "600",
  },
});
