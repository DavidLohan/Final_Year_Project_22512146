package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class HomeScreen {

    private final Scene scene;

    public HomeScreen(Stage stage) {

        Label title = new Label("Drawing Communication App");
        title.setFont(new Font(40));

        Label subtitle = new Label("Draw to communicate visually.");
        subtitle.setFont(new Font(18));

        Button startBtn = new Button("Start Drawing");
        Button viewSavedBtn = new Button("View Saved Drawings");
        Button exitBtn = new Button("Exit");

        startBtn.setMinWidth(220);
        viewSavedBtn.setMinWidth(220);
        exitBtn.setMinWidth(220);

        startBtn.setOnAction(e -> stage.setScene(new DrawingApp(stage).getScene()));

        viewSavedBtn.setOnAction(e -> stage.setScene(new CommunicationBoardScreen().createScene(stage)));

        exitBtn.setOnAction(e -> stage.close());

        VBox root = new VBox(18, title, subtitle, startBtn, viewSavedBtn, exitBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));

        scene = new Scene(root, 1000, 700);
    }

    public Scene getScene() {
        return scene;
    }
}