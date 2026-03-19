from sqlalchemy import Column, Integer, String, Float, DateTime
from sqlalchemy.sql import func
from database import Base


class PredictionRecord(Base):
    __tablename__ = "predictions"

    id = Column(Integer, primary_key=True, index=True)
    session_id = Column(String, index=True, nullable=False)
    predicted_label = Column(String, nullable=False)
    confidence = Column(Float, nullable=False)
    second_choice = Column(String, nullable=True)
    third_choice = Column(String, nullable=True)
    image_path = Column(String, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

