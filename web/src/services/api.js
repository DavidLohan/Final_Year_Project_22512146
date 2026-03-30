const API_BASE = `${window.location.protocol}//${window.location.hostname}:8000`;

export async function submitPrediction(blob, sessionId) {
  const formData = new FormData();
  formData.append("file", blob, "drawing.png");
  formData.append("session_id", sessionId);

  const response = await fetch(`${API_BASE}/predict`, {
    method: "POST",
    body: formData,
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data?.detail || "Prediction failed.");
  }

  return data;
}

export async function fetchHistory(sessionId) {
  const response = await fetch(`${API_BASE}/predictions/${sessionId}`);
  const data = await response.json();

  if (!response.ok) {
    throw new Error(data?.detail || "Could not load history.");
  }

  return data;
}

export async function deletePrediction(id) {
  const response = await fetch(`${API_BASE}/predictions/${id}`, {
    method: "DELETE",
  });

  let data = null;
  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    throw new Error(data?.detail || "Could not delete prediction.");
  }

  return data;
}

export function getImageUrl(imageUrl) {
  if (!imageUrl) return "";
  if (imageUrl.startsWith("http")) return imageUrl;
  return `${API_BASE}${imageUrl}`;
}