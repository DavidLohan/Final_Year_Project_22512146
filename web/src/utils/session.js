export function getSessionId() {
  let sessionId = localStorage.getItem("quickdraw_session_id");

  if (!sessionId) {
    sessionId = Math.random().toString(36).substring(2);
    localStorage.setItem("quickdraw_session_id", sessionId);
  }

  return sessionId;
}