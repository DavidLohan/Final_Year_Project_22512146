const canvas = document.getElementById("drawingCanvas");
const ctx = canvas.getContext("2d");

const clearBtn = document.getElementById("clearBtn");
const submitBtn = document.getElementById("submitBtn");
const refreshBtn = document.getElementById("refreshBtn");
const speakBtn = document.getElementById("speakBtn");

const statusMessage = document.getElementById("statusMessage");
const predictionBox = document.getElementById("predictionBox");
const predictedLabel = document.getElementById("predictedLabel");
const confidenceValue = document.getElementById("confidenceValue");
const top3List = document.getElementById("top3List");
const historyList = document.getElementById("historyList");

// Change this if you want to force a backend address for phone testing.
// Example: const API_BASE = "http://192.168.0.199:8000";
const API_BASE = `${window.location.protocol}//${window.location.hostname}:8000`;

let drawing = false;
let hasDrawn = false;
let lastPrediction = "";

function getSessionId() {
  let sessionId = localStorage.getItem("quickdraw_session_id");
  if (!sessionId) {
    sessionId = crypto.randomUUID();
    localStorage.setItem("quickdraw_session_id", sessionId);
  }
  return sessionId;
}

const sessionId = getSessionId();

function setupCanvas() {
  ctx.fillStyle = "white";
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  ctx.strokeStyle = "black";
  ctx.lineWidth = 12;
  ctx.lineCap = "round";
  ctx.lineJoin = "round";
}

function getPointerPos(event) {
  const rect = canvas.getBoundingClientRect();
  const scaleX = canvas.width / rect.width;
  const scaleY = canvas.height / rect.height;

  return {
    x: (event.clientX - rect.left) * scaleX,
    y: (event.clientY - rect.top) * scaleY
  };
}

function startDrawing(event) {
  drawing = true;
  const pos = getPointerPos(event);
  ctx.beginPath();
  ctx.moveTo(pos.x, pos.y);
}

function draw(event) {
  if (!drawing) return;
  event.preventDefault();

  const pos = getPointerPos(event);
  ctx.lineTo(pos.x, pos.y);
  ctx.stroke();
  hasDrawn = true;
}

function stopDrawing() {
  drawing = false;
  ctx.beginPath();
}

function setLoadingState(isLoading) {
  clearBtn.disabled = isLoading;
  submitBtn.disabled = isLoading;
  refreshBtn.disabled = isLoading;
}

function speakText(text) {
  if (!("speechSynthesis" in window)) {
    statusMessage.textContent = "Speech is not supported in this browser.";
    return;
  }

  window.speechSynthesis.cancel();

  const utterance = new SpeechSynthesisUtterance(text);
  utterance.rate = 1;
  utterance.pitch = 1;
  window.speechSynthesis.speak(utterance);
}

function formatDate(dateString) {
  if (!dateString) return "N/A";

  const date = new Date(dateString);
  if (Number.isNaN(date.getTime())) return dateString;

  return date.toLocaleString();
}

canvas.addEventListener("pointerdown", startDrawing);
canvas.addEventListener("pointermove", draw);
canvas.addEventListener("pointerup", stopDrawing);
canvas.addEventListener("pointerleave", stopDrawing);

clearBtn.addEventListener("click", () => {
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  setupCanvas();
  hasDrawn = false;
  lastPrediction = "";
  statusMessage.textContent = "";
  predictionBox.classList.add("hidden");
  speakBtn.disabled = true;
});

speakBtn.addEventListener("click", () => {
  if (lastPrediction) {
    speakText(`I think this is ${lastPrediction}`);
  }
});

submitBtn.addEventListener("click", async () => {
  if (!hasDrawn) {
    statusMessage.textContent = "Please draw something before submitting.";
    predictionBox.classList.add("hidden");
    return;
  }

  try {
    setLoadingState(true);
    statusMessage.textContent = "Submitting drawing...";
    predictionBox.classList.add("hidden");
    speakBtn.disabled = true;

    const blob = await new Promise((resolve) => {
      canvas.toBlob(resolve, "image/png");
    });

    const formData = new FormData();
    formData.append("file", blob, "drawing.png");
    formData.append("session_id", sessionId);

    const response = await fetch(`${API_BASE}/predict`, {
      method: "POST",
      body: formData
    });

    const data = await response.json();

    if (!response.ok) {
      statusMessage.textContent = data.detail || "Prediction failed.";
      return;
    }

    lastPrediction = data.label || "";
    predictedLabel.textContent = data.label || "-";
    confidenceValue.textContent =
      typeof data.confidence === "number"
        ? `${(data.confidence * 100).toFixed(2)}%`
        : "-";

    top3List.innerHTML = "";
    if (Array.isArray(data.top3)) {
      for (const item of data.top3) {
        const li = document.createElement("li");
        li.textContent = `${item.label} - ${(item.confidence * 100).toFixed(2)}%`;
        top3List.appendChild(li);
      }
    }

    predictionBox.classList.remove("hidden");
    statusMessage.textContent = "Prediction complete.";
    speakBtn.disabled = !lastPrediction;

    await loadHistory();
  } catch (error) {
    console.error(error);
    statusMessage.textContent = "Could not connect to the server.";
  } finally {
    setLoadingState(false);
  }
});

refreshBtn.addEventListener("click", loadHistory);

async function loadHistory() {
  try {
    historyList.innerHTML = "<p>Loading...</p>";

    const response = await fetch(`${API_BASE}/predictions/${sessionId}`);
    const data = await response.json();

    if (!response.ok) {
      historyList.innerHTML = "<p>Could not load history.</p>";
      return;
    }

    if (!Array.isArray(data) || data.length === 0) {
      historyList.innerHTML = "<p>No drawings saved yet.</p>";
      return;
    }

    historyList.innerHTML = "";

    for (const item of data) {
      const wrapper = document.createElement("div");
      wrapper.className = "history-item";

      const img = document.createElement("img");
      img.src = `${API_BASE}${item.imageUrl}`;
      img.alt = item.label || "Saved drawing";

      const meta = document.createElement("div");
      meta.className = "history-meta";
      meta.innerHTML = `
        <p><strong>Label:</strong> ${item.label ?? "Unknown"}</p>
        <p><strong>Confidence:</strong> ${
          typeof item.confidence === "number"
            ? `${(item.confidence * 100).toFixed(2)}%`
            : "N/A"
        }</p>
        <p><strong>Created:</strong> ${formatDate(item.createdAt)}</p>
        <button class="history-speak-btn">Speak</button>
      `;

      const speakHistoryBtn = meta.querySelector(".history-speak-btn");
      speakHistoryBtn.addEventListener("click", () => {
        if (item.label) {
          speakText(`This drawing was predicted as ${item.label}`);
        }
      });

      wrapper.appendChild(img);
      wrapper.appendChild(meta);
      historyList.appendChild(wrapper);
    }
  } catch (error) {
    console.error(error);
    historyList.innerHTML = "<p>Could not connect to the server.</p>";
  }
}

setupCanvas();
loadHistory();