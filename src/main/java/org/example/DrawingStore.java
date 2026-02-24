package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class DrawingStore {
    private static final DrawingStore INSTANCE = new DrawingStore();

    private final ObservableList<DrawingItem> drawings = FXCollections.observableArrayList();
    private final AtomicInteger counter = new AtomicInteger(1);

    private DrawingStore() {}

    public static DrawingStore getInstance() {
        return INSTANCE;
    }

    public ObservableList<DrawingItem> getDrawings() {
        return drawings;
    }

    public DrawingItem addDrawing(Image image) {
        String title = "Drawing " + counter.getAndIncrement();
        DrawingItem item = new DrawingItem(title, image, LocalDateTime.now());
        drawings.add(item);
        return item;
    }

    public void remove(DrawingItem item) {
        drawings.remove(item);
    }

    public void clearAll() {
        drawings.clear();
    }
}