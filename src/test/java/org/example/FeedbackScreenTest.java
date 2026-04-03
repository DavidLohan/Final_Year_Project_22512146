package org.example;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class FeedbackScreenTest {

    @BeforeAll
    static void initJavaFX() {
        new JFXPanel();
    }

    private static void runOnFxThreadAndWait(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });

        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("FX thread timeout");
        }
    }

    @Test
    void createSceneBuildsSceneSuccessfully() throws Exception {
        AtomicReference<Scene> sceneRef = new AtomicReference<>();

        runOnFxThreadAndWait(() -> {
            FeedbackScreen screen = new FeedbackScreen();

            PredictionResult result = new PredictionResult(
                    1, "cat", 0.9, "dog", "bird", "{}"
            );

            Scene scene = screen.createScene(
                    new Stage(),
                    new WritableImage(100, 100),
                    result
            );

            sceneRef.set(scene);
        });

        assertNotNull(sceneRef.get());
    }

    @Test
    void initialLabelsShowCorrectGuessAndConfidence() throws Exception {
        AtomicReference<String> guessText = new AtomicReference<>();
        AtomicReference<String> infoText = new AtomicReference<>();

        runOnFxThreadAndWait(() -> {
            FeedbackScreen screen = new FeedbackScreen();

            PredictionResult result = new PredictionResult(
                    1, "cat", 0.85, "dog", null, "{}"
            );

            Scene scene = screen.createScene(
                    new Stage(),
                    new WritableImage(100, 100),
                    result
            );

            Label guessLabel = (Label) scene.lookup(".section-label");
            Label infoLabel = (Label) scene.lookup(".info-label");

            guessText.set(guessLabel.getText());
            infoText.set(infoLabel.getText());
        });

        assertTrue(guessText.get().contains("cat"));
        assertTrue(infoText.get().contains("85.0%"));
    }

    @Test
    void incorrectButtonMovesToNextGuess() throws Exception {
        AtomicReference<String> updatedGuess = new AtomicReference<>();

        runOnFxThreadAndWait(() -> {
            FeedbackScreen screen = new FeedbackScreen();

            PredictionResult result = new PredictionResult(
                    1, "cat", 0.9, "dog", null, "{}"
            );

            Scene scene = screen.createScene(
                    new Stage(),
                    new WritableImage(100, 100),
                    result
            );

            Button incorrectBtn = (Button) scene.lookup(".secondary-button");
            Label guessLabel = (Label) scene.lookup(".section-label");

            incorrectBtn.fire();

            updatedGuess.set(guessLabel.getText());
        });

        assertTrue(updatedGuess.get().contains("dog"));
    }

    @Test
    void incorrectButtonAtLastGuessDoesNotCrash() throws Exception {
        runOnFxThreadAndWait(() -> {
            FeedbackScreen screen = new FeedbackScreen();

            PredictionResult result = new PredictionResult(
                    1, "cat", 0.9, null, null, "{}"
            );

            Scene scene = screen.createScene(
                    new Stage(),
                    new WritableImage(100, 100),
                    result
            );

            Button incorrectBtn = (Button) scene.lookup(".secondary-button");

            assertDoesNotThrow(incorrectBtn::fire);
        });
    }
}