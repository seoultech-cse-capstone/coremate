import {
  createContext,
  type PropsWithChildren,
  useContext,
  useMemo,
  useState,
} from "react";

type LogWeakSelectionContextValue = {
  isLogWeakSelecting: boolean;
  setIsLogWeakSelecting: (isSelecting: boolean) => void;
};

const LogWeakSelectionContext =
  createContext<LogWeakSelectionContextValue | null>(null);

export function LogWeakSelectionProvider({ children }: PropsWithChildren) {
  const [isLogWeakSelecting, setIsLogWeakSelecting] = useState(false);

  const value = useMemo(
    () => ({
      isLogWeakSelecting,
      setIsLogWeakSelecting,
    }),
    [isLogWeakSelecting],
  );

  return (
    <LogWeakSelectionContext.Provider value={value}>
      {children}
    </LogWeakSelectionContext.Provider>
  );
}

export function useLogWeakSelection() {
  const context = useContext(LogWeakSelectionContext);

  if (!context) {
    throw new Error(
      "useLogWeakSelection must be used within LogWeakSelectionProvider",
    );
  }

  return context;
}
