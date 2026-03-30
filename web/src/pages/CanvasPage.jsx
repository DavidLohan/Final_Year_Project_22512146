import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import Toolbar from "../components/Toolbar";
import FeedbackModal from "../components/FeedbackModal";
import { useBoard } from "../context/useBoard";
import { getImageUrl, submitPrediction } from "../services/api";
import { getSessionId } from "../utils/session";

export default function CanvasPage() {
  const navigate = useNavigate();
  const canvasRef = useRef(null);
  const isDrawingRef = useRef(false);
  const sessionId = useMemo(() => getSessionId(), []);
  const { addApprovedItem } = useBoard();

  const [isErasing, setIsErasing] = useState(false);
  const [hasDrawing, setHasDrawing] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedbackOpen, setFeedbackOpen] = useState(false);
  const [predictionData, setPredictionData] = useState(null);
  const [attemptIndex, setAttemptIndex] = useState(0);
  const [savedPredictionMeta, setSavedPredictionMeta] = useState(null);
  const [statusMessage, setStatusMessage] = useState("");

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.strokeStyle = "#111827";
    ctx.lineWidth = 10;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
  }, []);

  function getPointerPosition(event) {
    const canvas = canvasRef.current;
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;

    return {
      x: (event.clientX - rect.left) * scaleX,
      y: (event.clientY - rect.top) * scaleY,
    };
  }

  function handlePointerDown(event) {
  event.preventDefault();
  isDrawingRef.current = true;

  const ctx = canvasRef.current.getContext("2d");
  const point = getPointerPosition(event);

  ctx.beginPath();
  ctx.globalCompositeOperation = isErasing ? "destination-out" : "source-over";
  ctx.lineWidth = isErasing ? 24 : 10;
  ctx.lineCap = "round";
  ctx.lineJoin = "round";
  ctx.moveTo(point.x, point.y);
}

  function handlePointerMove(event) {
  if (!isDrawingRef.current) return;
  event.preventDefault();

  const ctx = canvasRef.current.getContext("2d");
  const point = getPointerPosition(event);

  ctx.globalCompositeOperation = isErasing ? "destination-out" : "source-over";
  ctx.lineWidth = isErasing ? 24 : 10;
  ctx.lineTo(point.x, point.y);
  ctx.stroke();

  setHasDrawing(true);
}

  function handlePointerUp(event) {
    event.preventDefault();
    isDrawingRef.current = false;
  }

  function clearCanvas() {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.globalCompositeOperation = "source-over";
    setHasDrawing(false);
    setStatusMessage("");
  }

  function canvasToBlob() {
    return new Promise((resolve) => {
      canvasRef.current.toBlob((blob) => resolve(blob), "image/png");
    });
  }

  async function handleSubmit() {
    if (!hasDrawing || isSubmitting) {
      setStatusMessage("Please draw something before submitting.");
      return;
      }

    try {
      setIsSubmitting(true);
      setStatusMessage("Submitting drawing...");
      const blob = await canvasToBlob();
      const result = await submitPrediction(blob, sessionId);

      setPredictionData(result);
      setSavedPredictionMeta({
        id: result.id ?? `${Date.now()}-${Math.random()}`,
        imageUrl: result.imageUrl ?? null,
        createdAt: result.createdAt ?? new Date().toISOString(),
      });
      setAttemptIndex(0);
      setFeedbackOpen(true);
      setStatusMessage("");
    } catch (error) {
      console.error(error);
      setStatusMessage(error.message || "Prediction failed.");
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleCorrect() {
    const chosen = predictionData?.top3?.[attemptIndex];
    if (!chosen) return;

    addApprovedItem({
      id: savedPredictionMeta?.id ?? `${Date.now()}-${Math.random()}`,
      backendId: savedPredictionMeta?.id ?? null,
      label: chosen.label,
      displayImage: getImageUrl(savedPredictionMeta?.imageUrl),
      rawImageUrl: savedPredictionMeta?.imageUrl,
      createdAtText: new Date(savedPredictionMeta?.createdAt ?? Date.now()).toLocaleString(),
    });

    setFeedbackOpen(false);
    setPredictionData(null);
    setSavedPredictionMeta(null);
    setAttemptIndex(0);
    clearCanvas();
  }

  function handleIncorrect() {
    const nextIndex = attemptIndex + 1;
    const nextPrediction = predictionData?.top3?.[nextIndex];

    if (nextPrediction && nextIndex < 3) {
      setAttemptIndex(nextIndex);
      return;
    }

    alert("Sorry, I could not identify that drawing.");
    setFeedbackOpen(false);
    setPredictionData(null);
    setSavedPredictionMeta(null);
    setAttemptIndex(0);
    clearCanvas();
  }

  function handleCloseModal() {
    setFeedbackOpen(false);
    setPredictionData(null);
    setSavedPredictionMeta(null);
    setAttemptIndex(0);
  }
  const currentLabel = predictionData?.top3?.[attemptIndex]?.label ?? null;

  return (
    <main className="screen">
      <div className="page-shell">
        <Toolbar
          onGoToBoard={() => navigate("/board")}
          onClear={clearCanvas}
          onSelectPencil={() => setIsErasing(false)}
          onSelectEraser={() => setIsErasing(true)}
          onSubmit={handleSubmit}
          isErasing={isErasing}
          isSubmitting={isSubmitting}
        />

        <section className="card canvas-card">
          <canvas
            ref={canvasRef}
            width={1200}
            height={700}
            className="drawing-canvas"
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            onPointerLeave={handlePointerUp}
          />

          {statusMessage ? <p className="status-message">{statusMessage}</p> : null}
        </section>
      </div>

      <FeedbackModal
        isOpen={feedbackOpen}
        currentLabel={currentLabel}
        attemptNumber={attemptIndex + 1}
        onCorrect={handleCorrect}
        onIncorrect={handleIncorrect}
        onClose={handleCloseModal}
      />
    </main>
  );
}