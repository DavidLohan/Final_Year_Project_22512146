import { useContext } from "react";
import { BoardContext } from "./BoardContext";

export function useBoard() {
  const context = useContext(BoardContext);

  if (!context) {
    throw new Error("useBoard must be used inside BoardProvider");
  }

  return context;
}