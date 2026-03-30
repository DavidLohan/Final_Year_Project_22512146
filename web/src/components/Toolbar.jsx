export default function Toolbar({
  onGoToBoard,
  onClear,
  onSelectPencil,
  onSelectEraser,
  onSubmit,
  isErasing,
  isSubmitting,
}) {
  return (
    <header className="toolbar card">
      <div className="toolbar-top">
        <h1 className="page-title">Canvas</h1>
        <button className="secondary-btn" type="button" onClick={onGoToBoard}>
          Go to Communication Board
        </button>
      </div>

      <div className="toolbar-actions">
        <div className="tool-buttons">
          <button
          type="button"
            className={`secondary-btn tool-btn ${!isErasing ? "active-tool" : ""}`}
            onClick={onSelectPencil}
            disabled={!isErasing}
          >
            Pencil
          </button>

          <button
          type="button"
            className={`secondary-btn tool-btn ${isErasing ? "active-tool" : ""}`}
            onClick={onSelectEraser}
            disabled={isErasing}
          >
            Eraser
          </button>
        </div>

        <button className="secondary-btn" type="button" onClick={onClear}>
          Clear
        </button>

        <button
          className="primary-btn"
          type="button"
          onClick={onSubmit}
          disabled={isSubmitting}
        >
          {isSubmitting ? "Submitting..." : "Submit"}
        </button>
      </div>
    </header>
  );
}