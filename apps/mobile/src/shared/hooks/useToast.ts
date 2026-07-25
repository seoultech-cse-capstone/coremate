import { useEffect, useRef, useState } from "react";
import { Animated } from "react-native";

/**
 * 화면 하단에 잠깐 떴다 사라지는 토스트 메시지. 사용하는 화면에서
 * `const { message, opacity, showToast } = useToast()`로 받아
 * `<Toast message={message} opacity={opacity} />`를 렌더하면 된다.
 */
export function useToast() {
  const [message, setMessage] = useState<string | null>(null);
  const opacity = useRef(new Animated.Value(0)).current;
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const showToast = (text: string) => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
    setMessage(text);
    opacity.setValue(0);
    Animated.timing(opacity, {
      toValue: 1,
      duration: 180,
      useNativeDriver: true,
    }).start();

    timeoutRef.current = setTimeout(() => {
      Animated.timing(opacity, {
        toValue: 0,
        duration: 220,
        useNativeDriver: true,
      }).start(({ finished }) => {
        if (finished) {
          setMessage(null);
        }
      });
    }, 1600);
  };

  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, []);

  return { message, opacity, showToast };
}
