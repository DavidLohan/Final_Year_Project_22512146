import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import SavedDrawingList from "../components/SavedDrawingList";
import { useBoard } from "../context/useBoard";
import { deletePrediction, fetchHistory, getImageUrl } from "../services/api";
import { getSessionId } from "../utils/session";

export default function CommunicationBoardPage() {
  const navigate = useNavigate();
  const sessionId = useMemo(() => getSessionId(), []);
  const { approvedItems, removeApprovedItem, clearApprovedItems } = useBoard();
  const [selectedId, setSelectedId] = useState(null);
  const [historyItems, setHistoryItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    async function loadHistory() {
      try {
        setLoading(true);
        setErrorMessage("");
        const data = await fetchHistory(sessionId);
        setHistoryItems(Array.isArray(data) ? data : []);
      } catch (error) {
        console.error(error);
        setErrorMessage("Could not load saved drawings from the backend.");
      } finally {
        setLoading(false);
      }
}

    loadHistory();
  }, [sessionId]);

  const mergedApprovedItems = useMemo(() => {
    return approvedItems.map((item) => {
      const backendMatch = historyItems.find((historyItem) => {
        if (item.backendId && historyItem.id) {
          return String(item.backendId) === String(historyItem.id);
        }
        if (item.rawImageUrl && historyItem.imageUrl) {
          return item.rawImageUrl === historyItem.imageUrl;
        }
        return false;
      });

      return {
        ...item,
        backendId: backendMatch?.id ?? item.backendId ?? null,
        displayImage: getImageUrl(backendMatch?.imageUrl ?? item.rawImageUrl ?? item.displayImage),
      };
    });
  }, [approvedItems, historyItems]);

  const selectedItem = useMemo(() => {
    return mergedApprovedItems.find((item) => item.id === selectedId) ?? mergedApprovedItems[0] ?? null;
  }, [mergedApprovedItems, selectedId]);

  function handleSelect(item) {
    setSelectedId(item.id);
  }
function handleSpeakSelected() {
    if (!selectedItem) return;
    const utterance = new SpeechSynthesisUtterance(selectedItem.label);
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(utterance);
  }

  async function handleDeleteSelected() {
    if (!selectedItem) return;

    try {
      if (selectedItem.backendId) {
        await deletePrediction(selectedItem.backendId);
      }
      removeApprovedItem(selectedItem.id);
      setSelectedId(null);
    } catch (error) {
      console.error(error);
      alert("Could not delete the selected drawing.");
    }
  }

  function handleClearAll() {
    clearApprovedItems();
    setSelectedId(null);
  }

  return (
    <main className="screen">
      <div className="page-shell">
        <header className="board-header card">
          <h1 className="page-title center">Communication Board</h1>
        </header>

        <section className="board-grid">
          <div className="card board-panel list-panel">
            {loading ? <p className="empty-text">Loading saved drawings...</p> : null}
            {errorMessage ? <p className="empty-text">{errorMessage}</p> : null}
            {!loading && !errorMessage ? (
              <SavedDrawingList
                items={mergedApprovedItems}
                selectedId={selectedItem?.id ?? null}
                onSelect={handleSelect}
              />
            ) : null}
          </div>

          <div className="card board-panel preview-panel">
            {selectedItem ? (
              <>
                <h2 className="preview-title">{selectedItem.label}</h2>
                <div className="preview-frame">
                  <img src={selectedItem.displayImage} alt={selectedItem.label} className="preview-image" />
                </div>
                <button className="primary-btn" type="button" onClick={handleSpeakSelected}>
                  Speak Selected
                </button>
              </>
            ) : (
              <p className="empty-text">Approve a drawing from the canvas page to show it here.</p>
            )}
          </div>
        </section>

        <div className="board-actions">
          <button className="secondary-btn" type="button" onClick={handleDeleteSelected} disabled={!selectedItem}>
            Delete Selected
          </button>
          <button className="secondary-btn" type="button" onClick={handleClearAll} disabled={!mergedApprovedItems.length}>
            Clear All
          </button>
          <button className="secondary-btn" type="button" onClick={() => navigate("/canvas")}>
            Back to Canvas
          </button>
        </div>
      </div>
    </main>
  );
}