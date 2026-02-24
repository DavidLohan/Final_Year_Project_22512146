package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class FeedbackScreen {

    public Scene createScene(Stage stage, Image submittedImage, String message) {
        Label title = new Label("Submission Feedback");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        ImageView preview = new ImageView(submittedImage);
        preview.setFitWidth(400);
        preview.setFitHeight(300);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);

        Label msg = new Label(message);
        msg.setWrapText(true);
        msg.setStyle("-fx-font-size: 14px;");

        Button goToBoard = new Button("Go to Communication Board");
        goToBoard.setOnAction(e -> stage.setScene(new CommunicationBoardScreen().createScene(stage)));

        Button backHome = new Button("Back to Home");
        backHome.setOnAction(e -> stage.setScene(new HomeScreen(stage).getScene()));

        VBox root = new VBox(12, title, preview, msg, goToBoard, backHome);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        return new Scene(root, 800, 600);
    }
}