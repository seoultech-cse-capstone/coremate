import { BlurView } from "expo-blur";
import { router, useNavigation } from "expo-router";
import { UserPlus } from "lucide-react-native";
import { useEffect, useRef, useState } from "react";
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
import { FriendListCard } from "../../src/features/friend/components/FriendListCard";
import { FRIENDS } from "../../src/features/friend/friendData";

const BRAND_PURPLE = "#6D5DFB";
const PANEL_GRAY = "#F3F3F3";
const OVERSCROLL_BUFFER = 96;

type TabPressNavigation = {
  addListener: (eventName: "tabPress", callback: () => void) => () => void;
};

export default function FriendScreen() {
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const scrollRef = useRef<ScrollView>(null);
  const { height } = useWindowDimensions();
  const [bounceBackground, setBounceBackground] = useState(BRAND_PURPLE);
  const [pokedFriendIds, setPokedFriendIds] = useState<string[]>([]);
  const panelMinHeight = Math.max(0, height - (60 + insets.top));
  const onlineFriends = FRIENDS.filter((friend) => friend.isOnline);
  const offlineFriends = FRIENDS.filter((friend) => !friend.isOnline);

  useEffect(() => {
    const tabNavigation = navigation as unknown as TabPressNavigation;
    const unsubscribe = tabNavigation.addListener("tabPress", () => {
      scrollRef.current?.scrollTo({ y: 0, animated: true });
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

  const togglePoke = (friendId: string) => {
    setPokedFriendIds((current) =>
      current.includes(friendId)
        ? current.filter((id) => id !== friendId)
        : [...current, friendId],
    );
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
          accessibilityLabel="친구 추가"
          accessibilityRole="button"
          hitSlop={10}
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
            <FriendSection
              friends={onlineFriends}
              isPoked={(friendId) => pokedFriendIds.includes(friendId)}
              onOpenProfile={(friendId) =>
                router.push({ pathname: "/friend-profile", params: { friendId } })
              }
              onPoke={togglePoke}
              title="Online Now"
              statusColor="#22C55E"
            />
            <FriendSection
              friends={offlineFriends}
              isPoked={(friendId) => pokedFriendIds.includes(friendId)}
              onOpenProfile={(friendId) =>
                router.push({ pathname: "/friend-profile", params: { friendId } })
              }
              onPoke={togglePoke}
              title="Offline Now"
              statusColor="#94A3B8"
            />
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

type FriendSectionProps = {
  friends: typeof FRIENDS;
  title: string;
  statusColor: string;
  isPoked: (friendId: string) => boolean;
  onOpenProfile: (friendId: string) => void;
  onPoke: (friendId: string) => void;
};

function FriendSection({
  friends,
  title,
  statusColor,
  isPoked,
  onOpenProfile,
  onPoke,
}: FriendSectionProps) {
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
            isPoked={isPoked(friend.id)}
            key={friend.id}
            onPoke={() => onPoke(friend.id)}
            onPress={() => onOpenProfile(friend.id)}
          />
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
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
});
