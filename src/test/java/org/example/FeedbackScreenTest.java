package org.example;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class FeedbackScreenTest {

    private Stage stage;
    private PredictionResult result;

    @Start
    private void start(Stage stage) {
        this.stage = stage;

        result = Mockito.mock(PredictionResult.class);
        Mockito.when(result.getCurrentGuess()).thenReturn("cat");
        Mockito.when(result.getConfidence()).thenReturn(0.92);

        WritableImage image = new WritableImage(100, 100);
        Scene scene = new FeedbackScreen().createScene(stage, image, result);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void notCorrectUntilNoMoreGuessesDisablesButton() throws Exception {
        Button notCorrectBtn = (Button) stage.getScene().lookup("#notCorrectBtn");
        Label guessLabel = (Label) stage.getScene().lookup("#guessLabel");
        Label infoLabel = (Label) stage.getScene().lookup("#infoLabel");

        assertNotNull(notCorrectBtn);
        assertNotNull(guessLabel);
        assertNotNull(infoLabel);

        Mockito.when(result.tryNextGuess()).thenReturn(false);

        runOnFxThread(notCorrectBtn::fire);

        assertTrue(guessLabel.getText().contains("No more guesses available."));
        assertTrue(notCorrectBtn.isDisabled());
        assertTrue(infoLabel.getText().contains("could not confidently identify"));
    }

    private void runOnFxThread(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        WaitForAsyncUtils.waitForFxEvents();
    }
}