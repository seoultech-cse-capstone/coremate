import { BlurView } from "expo-blur";
import { router, useNavigation } from "expo-router";
import { Settings } from "lucide-react-native";
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
import { useRequireMember } from "../../src/features/auth";
import { MyProfile } from "../../src/features/mypage/components/MyProfile";
import { MyProgressStats } from "../../src/features/mypage/components/MyProgressStats";
import { refetchActiveQueries } from "../../src/shared/api/queryClient";

const BRAND_PURPLE = "#6D5DFB";
const PANEL_GRAY = "#F3F3F3";
const OVERSCROLL_BUFFER = 96;

type TabPressNavigation = {
  addListener: (eventName: "tabPress", callback: () => void) => () => void;
};

export default function MyPageScreen() {
  useRequireMember();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const scrollRef = useRef<ScrollView>(null);
  const { height } = useWindowDimensions();
  const [bounceBackground, setBounceBackground] = useState(BRAND_PURPLE);
  const panelMinHeight = Math.max(0, height - (60 + insets.top));

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

    setBounceBackground((currentBackground) =>
      currentBackground === nextBackground ? currentBackground : nextBackground,
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: bounceBackground }]}>
      <BlurView
        intensity={28}
        tint="dark"
        style={[
          styles.titleArea,
          {
            minHeight: 60 + insets.top,
            paddingTop: insets.top,
          },
        ]}
      >
        <View style={styles.headerOverlay} />
        <Text style={styles.title}>My Page</Text>

        <Pressable
          accessibilityLabel="설정"
          accessibilityRole="button"
          hitSlop={10}
          style={[styles.settingsButton, { top: 9 + insets.top }]}
          onPress={() => router.navigate("/settings")}
        >
          <Settings color="#ffffff" size={25} strokeWidth={2.2} />
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
        <View style={styles.profileArea}>
          <MyProfile />
        </View>

        <View style={[styles.panel, { minHeight: panelMinHeight }]}>
          <View style={styles.panelContent}>
            <MyProgressStats />
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  titleArea: {
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    zIndex: 10,
    minHeight: 60,
    flexDirection: "row",
    alignItems: "center",
    overflow: "hidden",
    paddingHorizontal: 16,
    paddingTop: 0,
  },
  headerOverlay: {
    ...StyleSheet.absoluteFill,
    backgroundColor: "rgba(109, 93, 251, 0.85)",
  },
  title: {
    flexShrink: 1,
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 24,
    fontWeight: "700",
  },
  profileArea: {
    justifyContent: "center",
    paddingHorizontal: 24,
    paddingTop: 12,
    paddingBottom: 28,
  },
  settingsButton: {
    position: "absolute",
    top: 24,
    right: 16,
    alignItems: "center",
    justifyContent: "center",
    width: 42,
    height: 42,
    zIndex: 10,
    elevation: 10,
  },
  scroller: {
    flex: 1,
  },
  scrollContent: {
    backgroundColor: BRAND_PURPLE,
  },
  panel: {
    width: "100%",
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: PANEL_GRAY,
    overflow: "hidden",
  },
  panelContent: {
    backgroundColor: PANEL_GRAY,
    paddingHorizontal: 24,
    paddingTop: 32,
    paddingBottom: 28 + OVERSCROLL_BUFFER,
  },
});
