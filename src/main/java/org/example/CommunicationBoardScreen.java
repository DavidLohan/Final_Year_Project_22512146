package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CommunicationBoardScreen {

    public Scene createScene(Stage stage) {
        System.out.println("CommunicationBoardScreen opened");
        System.out.println("Session in board: " + SessionManager.getSessionId());

        Label title = new Label("Communication Board");
        title.getStyleClass().add("title-label");

        ObservableList<DrawingItem> drawings = FXCollections.observableArrayList();

        try {
            drawings.addAll(BackendService.fetchDrawings());
            System.out.println("Drawings loaded into board: " + drawings.size());
        } catch (Exception e) {
            e.printStackTrace();
        }

        ListView<DrawingItem> listView = new ListView<>(drawings);

        listView.setCellFactory(lv -> new ListCell<>() {
            private final HBox row = new HBox(12);
            private final ImageView thumb = new ImageView();
            private final VBox textBox = new VBox(4);
            private final Label name = new Label();
            private final Label date = new Label();

            {
                thumb.setFitWidth(90);
                thumb.setFitHeight(70);
                thumb.setPreserveRatio(true);
                thumb.setSmooth(true);

                name.getStyleClass().add("section-label");
                date.getStyleClass().add("info-label");

                textBox.getChildren().addAll(name, date);

                row.setAlignment(Pos.CENTER_LEFT);
                row.getChildren().addAll(thumb, textBox);
                row.setPadding(new Insets(8));
            }

            @Override
            protected void updateItem(DrawingItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Image image = item.getImage();
                thumb.setImage(image);

                name.setText(item.getTitle() != null ? item.getTitle() : "Untitled");
                date.setText(item.getCreatedAt() != null
                        ? item.getCreatedAt().toString()
                        : "No date");

                setText(null);
                setGraphic(row);
            }
        });

        listView.setPrefWidth(340);

        ImageView preview = new ImageView();
        preview.setFitWidth(420);
        preview.setFitHeight(320);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);

        Label previewLabel = new Label("Select a drawing to preview");
        previewLabel.getStyleClass().add("info-label");

        Button speakSelected = new Button("Speak Selected");
        speakSelected.getStyleClass().add("primary-button");
        speakSelected.setDisable(true);

        VBox previewImageBox = new VBox(preview);
        previewImageBox.setAlignment(Pos.CENTER);
        previewImageBox.getStyleClass().add("canvas-box");

        VBox previewBox = new VBox(12, previewLabel, previewImageBox, speakSelected);
        previewBox.setAlignment(Pos.TOP_CENTER);
        previewBox.getStyleClass().add("card");
        previewBox.setPrefWidth(460);

        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                preview.setImage(null);
                previewLabel.setText("Select a drawing to preview");
                speakSelected.setDisable(true);
            } else {
                preview.setImage(selected.getImage());
                previewLabel.setText(selected.getTitle());
                speakSelected.setDisable(false);
            }
        });

        speakSelected.setOnAction(e -> {
            DrawingItem selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getTitle() != null && !selected.getTitle().isBlank()) {
                TextToSpeechUtil.speak(selected.getTitle());
            }
        });

        VBox listBox = new VBox(12, listView);
        listBox.getStyleClass().add("card");
        HBox.setHgrow(listBox, Priority.ALWAYS);

        Button delete = new Button("Delete Selected");
        Button clearAll = new Button("Clear All");
        Button backHome = new Button("Back to Home");

        delete.getStyleClass().add("secondary-button");
        clearAll.getStyleClass().add("secondary-button");
        backHome.getStyleClass().add("secondary-button");

        delete.setOnAction(e -> {
            DrawingItem selected = listView.getSelectionModel().getSelectedItem();

            if (selected == null) {
                return;
            }
            Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Delete the selected drawing?",
                    ButtonType.YES,
                    ButtonType.NO
            );

            alert.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    try {
                        BackendService.deleteDrawing(selected.getId());
                        drawings.remove(selected);
                        preview.setImage(null);
                        previewLabel.setText("Select a drawing to preview");
                        speakSelected.setDisable(true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        new Alert(
                                Alert.AlertType.ERROR,
                                "Could not delete the drawing from the backend."
                        ).showAndWait();
                    }
                }
            });
        });

        clearAll.setOnAction(e -> {
            Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Remove all drawings from the board and database?",
                    ButtonType.YES,
                    ButtonType.NO
            );

            alert.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    try {
                        BackendService.deleteAllDrawingsForSession(SessionManager.getSessionId());
                        drawings.clear();
                        preview.setImage(null);
                        previewLabel.setText("Select a drawing to preview");
                        speakSelected.setDisable(true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        new Alert(
                                Alert.AlertType.ERROR,
                                "Could not clear drawings from the backend."
                        ).showAndWait();
                    }
                }
            });
        });

        backHome.setOnAction(e -> stage.setScene(new HomeScreen(stage).getScene()));

        HBox buttons = new HBox(12, delete, clearAll, backHome);
        buttons.setAlignment(Pos.CENTER);

        HBox content = new HBox(20, listBox, previewBox);
        content.setAlignment(Pos.CENTER);
        HBox.setHgrow(listBox, Priority.ALWAYS);

        VBox root = new VBox(20, title, content, buttons);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);

        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(
                getClass().getResource("/design/style.css").toExternalForm()
        );

        return scene;
    }
}