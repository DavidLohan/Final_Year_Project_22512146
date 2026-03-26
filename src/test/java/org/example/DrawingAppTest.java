package org.example;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class DrawingAppTest {

    private Stage stage;
    private Scene scene;

    @Start
    private void start(Stage stage) {
        this.stage = stage;
        DrawingApp app = new DrawingApp(stage);
        this.scene = app.getScene();
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void submitWithBlankCanvasShowsWarning() throws Exception {
        Button submitBtn = (Button) scene.lookup("#submitBtn");
        Label infoLabel = (Label) scene.lookup("#feedbackLabel");

        assertNotNull(submitBtn);
        assertNotNull(infoLabel);

        runOnFxThread(submitBtn::fire);

        assertTrue(infoLabel.getText().contains("Nothing to submit"));
    }

    @Test
    void backendTurnedOffShowsPredictionFailedMessage() throws Exception {
        Button submitBtn = (Button) scene.lookup("#submitBtn");
        Canvas canvas = (Canvas) scene.lookup("#drawingCanvas");
        Label infoLabel = (Label) scene.lookup("#feedbackLabel");

        assertNotNull(submitBtn);
        assertNotNull(canvas);
        assertNotNull(infoLabel);

        runOnFxThread(() -> canvas.getGraphicsContext2D().fillOval(50, 50, 20, 20));

        try (MockedStatic<PredictionClient> mockedPrediction = Mockito.mockStatic(PredictionClient.class)) {
            mockedPrediction.when(() -> PredictionClient.predict(Mockito.any()))
                    .thenThrow(new RuntimeException("Connection refused"));

            runOnFxThread(submitBtn::fire);

            assertTrue(infoLabel.getText().contains("Prediction failed"));
        }
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