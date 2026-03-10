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

    public Scene createScene(Stage stage, Image submittedImage, DrawingItem savedItem, PredictionResult result) {
        Label title = new Label("Submission Feedback");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        ImageView preview = new ImageView(submittedImage);
        preview.setFitWidth(400);
        preview.setFitHeight(300);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);

        Label guessLabel = new Label();
        guessLabel.setWrapText(true);
        guessLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label infoLabel = new Label();
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-font-size: 14px;");

        Runnable refreshText = () -> {
            guessLabel.setText("Is this a: " + result.getCurrentGuess() + "?");
            infoLabel.setText("Saved drawing: " + savedItem.getTitle()
                    + "\nModel confidence: " + String.format("%.1f%%", result.getConfidence() * 100));
        };

        refreshText.run();

        Button correctBtn = new Button("Correct");
        Button notCorrectBtn = new Button("Not Correct");
        Button goToBoard = new Button("Go to Communication Board");
        Button backHome = new Button("Back to Home");

        correctBtn.setOnAction(e -> {
            savedItem.setTitle(result.getCurrentGuess());
            stage.setScene(new CommunicationBoardScreen().createScene(stage));
        });

        notCorrectBtn.setOnAction(e -> {
            boolean hasAnotherGuess = result.tryNextGuess();
            if (hasAnotherGuess) {
                refreshText.run();
            } else {
                guessLabel.setText("No more guesses available.");
                infoLabel.setText("The model could not confidently identify this drawing."
                        + "\nYou can keep it as \"" + savedItem.getTitle() + "\" for now.");
                notCorrectBtn.setDisable(true);
            }
        });

        goToBoard.setOnAction(e -> stage.setScene(new CommunicationBoardScreen().createScene(stage)));
        backHome.setOnAction(e -> stage.setScene(new HomeScreen(stage).getScene()));

        VBox root = new VBox(12, title, preview, guessLabel, infoLabel,
                correctBtn, notCorrectBtn, goToBoard, backHome);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        return new Scene(root, 800, 650);
    }
}