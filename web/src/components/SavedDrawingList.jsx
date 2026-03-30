export default function SavedDrawingList({ items, selectedId, onSelect }) {
  if (!items.length) {
    return <p className="empty-text">No approved drawings saved yet.</p>;
  }

  return (
    <div className="saved-list">
      {items.map((item) => (
        <button
          key={item.id}
          type="button"
          className={`saved-item ${selectedId === item.id ? "selected" : ""}`}
          onClick={() => onSelect(item)}
        >
          <img src={item.displayImage} alt={item.label} className="saved-thumb" />
          <div className="saved-meta">
            <span className="saved-label">{item.label}</span>
            <span className="saved-date">{item.createdAtText}</span>
          </div>
        </button>
      ))}
    </div>
  );
}