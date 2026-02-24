package org.example;

import javafx.beans.property.*;
import javafx.scene.image.Image;

import java.time.LocalDateTime;

public class DrawingItem {
    private final StringProperty title = new SimpleStringProperty();
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();

    public DrawingItem(String title, Image image, LocalDateTime createdAt) {
        this.title.set(title);
        this.image.set(image);
        this.createdAt.set(createdAt);
    }

    public String getTitle() { return title.get(); }
    public Image getImage() { return image.get(); }
    public LocalDateTime getCreatedAt() { return createdAt.get(); }

    public StringProperty titleProperty() { return title; }
    public ObjectProperty<Image> imageProperty() { return image; }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }
}