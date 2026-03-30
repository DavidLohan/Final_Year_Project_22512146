import { Navigate, Route, Routes } from "react-router-dom";
import StartPage from "./pages/StartPage";
import CanvasPage from "./pages/CanvasPage";
import CommunicationBoardPage from "./pages/CommunicationBoardPage";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<StartPage />} />
      <Route path="/canvas" element={<CanvasPage />} />
      <Route path="/board" element={<CommunicationBoardPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}