import { useNavigate } from "react-router-dom";

export default function StartPage() {
  const navigate = useNavigate();

  return (
    <main className="screen screen-center">
      <div className="start-panel card">
        <h1 className="hero-title">Doodle Mind</h1>
        <p className="hero-subtitle">Draw to communicate visually</p>

        <div className="start-actions">
          <button className="primary-btn large-btn" type="button" onClick={() => navigate("/canvas")}>
            Start Drawing
          </button>
          <button className="secondary-btn large-btn" type="button" onClick={() => navigate("/board")}>
            Open Communication Board
          </button>
        </div>
      </div>
    </main>
  );
}