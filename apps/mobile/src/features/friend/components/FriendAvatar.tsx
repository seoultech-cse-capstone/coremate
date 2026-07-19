import { UserRound } from "lucide-react-native";
import { StyleSheet, View } from "react-native";

type FriendAvatarProps = {
  backgroundColor: string;
  color: string;
  size?: number;
};

export function FriendAvatar({
  backgroundColor,
  color,
  size = 44,
}: FriendAvatarProps) {
  return (
    <View
      style={[
        styles.container,
        {
          width: size,
          height: size,
          borderRadius: size / 2,
          backgroundColor,
        },
      ]}
    >
      <UserRound color={color} size={size * 0.5} strokeWidth={2.2} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: "center",
    justifyContent: "center",
  },
});
