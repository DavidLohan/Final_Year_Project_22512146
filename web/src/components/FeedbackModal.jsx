export default function FeedbackModal({
  isOpen,
  currentLabel,
  attemptNumber,
  onCorrect,
  onIncorrect,
  onSpeak,
  onClose,
}) {
  if (!isOpen || !currentLabel) return null;

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="feedback-title">
      <div className="modal-card">
        <h2 id="feedback-title">Feedback</h2>
        <p className="modal-text">
          Is this drawing a <strong>{currentLabel}</strong>?
        </p>
        <p className="modal-subtext">Attempt {attemptNumber} of 3</p>

        <div className="modal-actions">
          <button className="primary-btn" type="button" onClick={onSpeak}>
            Speak Word
          </button>
          <button className="primary-btn" type="button" onClick={onCorrect}>
            Correct
          </button>
          <button className="secondary-btn" type="button" onClick={onIncorrect}>
            Incorrect
          </button>
        </div>

        <button className="text-btn" type="button" onClick={onClose}>
          Close
        </button>
      </div>
    </div>
  );
}