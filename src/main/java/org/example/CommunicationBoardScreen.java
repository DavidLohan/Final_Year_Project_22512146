package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class CommunicationBoardScreen {

    public Scene createScene(Stage stage) {
        Label title = new Label("Communication Board");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        ListView<DrawingItem> listView = new ListView<>(DrawingStore.getInstance().getDrawings());
        listView.setCellFactory(lv -> new ListCell<>() {
            private final HBox row = new HBox(10);
            private final ImageView thumb = new ImageView();
            private final VBox textBox = new VBox(3);
            private final Label name = new Label();
            private final Label date = new Label();

            {
                thumb.setFitWidth(90);
                thumb.setFitHeight(70);
                thumb.setPreserveRatio(true);

                name.setStyle("-fx-font-weight: bold;");
                date.setStyle("-fx-font-size: 11px; -fx-opacity: 0.7;");

                textBox.getChildren().addAll(name, date);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getChildren().addAll(thumb, textBox);
            }

            @Override
            protected void updateItem(DrawingItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    thumb.setImage(item.getImage());
                    name.setText(item.getTitle());
                    date.setText(item.getCreatedAt().toString());
                    setGraphic(row);
                }
            }
        });

        // Right-side preview
        ImageView preview = new ImageView();
        preview.setFitWidth(420);
        preview.setFitHeight(320);
        preview.setPreserveRatio(true);

        Label previewLabel = new Label("Select a drawing to preview");
        previewLabel.setStyle("-fx-opacity: 0.7;");

        VBox previewBox = new VBox(10, previewLabel, preview);
        previewBox.setAlignment(Pos.TOP_CENTER);

        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                preview.setImage(null);
                previewLabel.setText("Select a drawing to preview");
            } else {
                preview.setImage(selected.getImage());
                previewLabel.setText(selected.getTitle());
            }
        });

        Button delete = new Button("Delete Selected");
        delete.setOnAction(e -> {
            DrawingItem selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                DrawingStore.getInstance().remove(selected);
            }
        });

        Button clearAll = new Button("Clear All");
        clearAll.setOnAction(e -> {
            Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Delete all saved drawings?",
                    ButtonType.YES,
                    ButtonType.NO
            );
            alert.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    DrawingStore.getInstance().clearAll();
                    preview.setImage(null);
                    previewLabel.setText("Select a drawing to preview");
                }
            });
        });

        Button backHome = new Button("Back to Home");
        backHome.setOnAction(e -> stage.setScene(new HomeScreen(stage).getScene()));

        HBox buttons = new HBox(10, delete, clearAll, backHome);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        HBox content = new HBox(15, listView, previewBox);
        content.setPadding(new Insets(15));
        HBox.setHgrow(listView, Priority.ALWAYS);
        listView.setPrefWidth(340);

        VBox root = new VBox(10, title, content, buttons);
        root.setPadding(new Insets(20));

        return new Scene(root, 900, 650);
    }
}