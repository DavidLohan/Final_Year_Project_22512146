import { useEffect, useMemo, useState } from "react";
import { BoardContext } from "./BoardContext";

export function BoardProvider({ children }) {
      const STORAGE_KEY = "approved_board_items";
  const [approvedItems, setApprovedItems] = useState(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
  if (!stored) return [];

  try {
    return JSON.parse(stored);
  } catch {
    return [];
  }
});

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(approvedItems));
  }, [approvedItems]);

  const value = useMemo(() => ({
    approvedItems,
    addApprovedItem: (item) => {
      setApprovedItems((prev) => [item, ...prev]);
    },
    removeApprovedItem: (id) => {
      setApprovedItems((prev) =>
        prev.filter((item) => item.id !== id)
      );
    },
    clearApprovedItems: () => {
      setApprovedItems([]);
    },
  }), [approvedItems]);

  return (
    <BoardContext.Provider value={value}>
      {children}
    </BoardContext.Provider>
  );
}