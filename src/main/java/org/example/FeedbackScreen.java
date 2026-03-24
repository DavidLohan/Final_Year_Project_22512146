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

    public Scene createScene(Stage stage, Image submittedImage, PredictionResult result) {
        Label title = new Label("Submission Feedback");
        title.getStyleClass().add("title-label");

        ImageView preview = new ImageView(submittedImage);
        preview.setFitWidth(400);
        preview.setFitHeight(300);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);

        VBox imageBox = new VBox(preview);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.getStyleClass().add("canvas-box");

        Label guessLabel = new Label();
        guessLabel.setWrapText(true);
        guessLabel.getStyleClass().add("section-label");

        Label infoLabel = new Label();
        infoLabel.setWrapText(true);
        infoLabel.getStyleClass().add("info-label");

        Runnable refreshText = () -> {
            guessLabel.setText("Is this a: " + result.getCurrentGuess() + "?");
            infoLabel.setText(
                    "Current selected label: " + result.getCurrentGuess()
                            + "\nModel confidence: " + String.format("%.1f%%", result.getConfidence() * 100)
            );
        };

        refreshText.run();

        Button speakBtn = new Button("Speak Word");
        Button correctBtn = new Button("Correct");
        Button notCorrectBtn = new Button("Not Correct");
        Button goToBoard = new Button("Go to Communication Board");
        Button backHome = new Button("Back to Home");

        speakBtn.getStyleClass().add("primary-button");
        correctBtn.getStyleClass().add("primary-button");
        notCorrectBtn.getStyleClass().add("secondary-button");
        goToBoard.getStyleClass().add("secondary-button");
        backHome.getStyleClass().add("secondary-button");

        speakBtn.setMinWidth(220);
        correctBtn.setMinWidth(220);
        notCorrectBtn.setMinWidth(220);
        goToBoard.setMinWidth(220);
        backHome.setMinWidth(220);

        speakBtn.setOnAction(e -> {
            try {
                TextToSpeechUtil.speak(result.getCurrentGuess());
            } catch (Exception ex) {
                infoLabel.setText("Could not speak the word.\nError: " + ex.getMessage());
            }
        });

        correctBtn.setOnAction(e -> {
            try {
                BackendService.updateDrawingLabel(
                        result.getPredictionId(),
                        result.getCurrentGuess()
                );
                stage.setScene(new CommunicationBoardScreen().createScene(stage));
            } catch (Exception ex) {
                guessLabel.setText("Could not save the confirmed label.");
                infoLabel.setText("Error: " + ex.getMessage());
            }
        });

        notCorrectBtn.setOnAction(e -> {
            boolean hasAnotherGuess = result.tryNextGuess();
            if (hasAnotherGuess) {
                refreshText.run();
            } else {
                guessLabel.setText("No more guesses available.");
                infoLabel.setText(
                        "The model could not confidently identify this drawing."
                                + "\nYou can go back and try drawing it more clearly."
                );
                notCorrectBtn.setDisable(true);
                speakBtn.setDisable(true);
            }
        });

        goToBoard.setOnAction(e -> stage.setScene(new CommunicationBoardScreen().createScene(stage)));
        backHome.setOnAction(e -> stage.setScene(new HomeScreen(stage).getScene()));

        VBox card = new VBox(15, imageBox, guessLabel, infoLabel,
                speakBtn, correctBtn, notCorrectBtn, goToBoard, backHome);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(500);
        card.getStyleClass().add("card");

        VBox root = new VBox(20, title, card);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 900, 700);
        scene.getStylesheets().add(
                getClass().getResource("/design/style.css").toExternalForm()
        );

        return scene;
    }
}