package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class HomeScreen {
    private final Scene scene;

    public HomeScreen(Stage stage) {
        Label title = new Label("Final Year Project for anomic aphasia");
        title.setFont(Font.font(28));

        Label subtitle = new Label("Tap Start to begin drawing");
        subtitle.setFont(Font.font(16));

        Button startBtn = new Button("Start Drawing");
        startBtn.setPrefWidth(240);

        Button exitBtn = new Button("Exit");
        exitBtn.setPrefWidth(240);

        startBtn.setOnAction(e -> stage.setScene(new DrawingApp(stage).getScene()));
        exitBtn.setOnAction(e -> stage.close());

        VBox box = new VBox(14, title, subtitle, startBtn, exitBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(25));
        box.setMaxWidth(420);
        box.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 16;
            -fx-border-color: #dddddd;
            -fx-border-radius: 16;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 12, 0.2, 0, 2);
            """);

        StackPane root = new StackPane(box);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f5f6fa;");

        scene = new Scene(root, 1000, 700);
    }

    public Scene getScene() {
        return scene;
    }
}
