package org.example;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class DrawingAppTest {

    @BeforeAll
    static void initJavaFX() {
        new JFXPanel();
    }

    private static void runOnFxThreadAndWait(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        if (!finished) {
            fail("Timed out waiting for JavaFX thread.");
        }

        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
    }

    @Test
    void getSceneReturnsScene() throws Exception {
        AtomicReference<Scene> sceneRef = new AtomicReference<>();

        runOnFxThreadAndWait(() -> {
            DrawingApp app = new DrawingApp(new Stage());
            sceneRef.set(app.getScene());
        });

        assertNotNull(sceneRef.get());
    }

    @Test
    void clearCanvasFillsCanvasWhite() throws Exception {
        AtomicReference<Color> centerColor = new AtomicReference<>();

        runOnFxThreadAndWait(() -> {
            DrawingApp app = new DrawingApp(new Stage());

            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, 100, 100);

            try {
                Method clearCanvas = DrawingApp.class.getDeclaredMethod("clearCanvas", GraphicsContext.class, Canvas.class);
                clearCanvas.setAccessible(true);
                clearCanvas.invoke(app, gc, canvas);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            WritableImage image = new WritableImage(100, 100);
            canvas.snapshot(null, image);
            PixelReader pr = image.getPixelReader();
            centerColor.set(pr.getColor(50, 50));
        });

        assertNotNull(centerColor.get());
        assertEquals(1.0, centerColor.get().getRed(), 0.01);
        assertEquals(1.0, centerColor.get().getGreen(), 0.01);
        assertEquals(1.0, centerColor.get().getBlue(), 0.01);
    }

    @Test
    void drawDotUsesBrushColorWhenEraserOff() throws Exception {
        AtomicReference<Color> drawnColor = new AtomicReference<>();

        runOnFxThreadAndWait(() -> {
            DrawingApp app = new DrawingApp(new Stage());

            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, 100, 100);

            try {
                Field eraserOn = DrawingApp.class.getDeclaredField("eraserOn");
                eraserOn.setAccessible(true);
                eraserOn.set(app, false);

                Field brushSize = DrawingApp.class.getDeclaredField("brushSize");
                brushSize.setAccessible(true);
                brushSize.set(app, 10.0);

                Field brushColor = DrawingApp.class.getDeclaredField("brushColor");
                brushColor.setAccessible(true);
                brushColor.set(app, Color.RED);

                MouseEvent event = new MouseEvent(
                        MouseEvent.MOUSE_PRESSED,
                        50, 50, 50, 50,
                        MouseButton.PRIMARY, 1,
                        false, false, false, false,
                        true, false, false, true, false, false,
                        null
                );

                Method drawDot = DrawingApp.class.getDeclaredMethod("drawDot", GraphicsContext.class, MouseEvent.class);
                drawDot.setAccessible(true);
                drawDot.invoke(app, gc, event);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            WritableImage image = new WritableImage(100, 100);
            canvas.snapshot(null, image);
            PixelReader pr = image.getPixelReader();
            drawnColor.set(pr.getColor(50, 50));
        });

        assertNotNull(drawnColor.get());
        assertTrue(drawnColor.get().getRed() > 0.8);
        assertTrue(drawnColor.get().getGreen() < 0.3);
        assertTrue(drawnColor.get().getBlue() < 0.3);
    }

    @Test
    void drawDotUsesWhiteWhenEraserOn() throws Exception {
        AtomicReference<Color> drawnColor = new AtomicReference<>();

        runOnFxThreadAndWait(() -> {
            DrawingApp app = new DrawingApp(new Stage());

            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, 100, 100);

            try {
                Field eraserOn = DrawingApp.class.getDeclaredField("eraserOn");
                eraserOn.setAccessible(true);
                eraserOn.set(app, true);

                Field brushSize = DrawingApp.class.getDeclaredField("brushSize");
                brushSize.setAccessible(true);
                brushSize.set(app, 10.0);

                MouseEvent event = new MouseEvent(
                        MouseEvent.MOUSE_PRESSED,
                        50, 50, 50, 50,
                        MouseButton.PRIMARY, 1,
                        false, false, false, false,
                        true, false, false, true, false, false,
                        null
                );

                Method drawDot = DrawingApp.class.getDeclaredMethod("drawDot", GraphicsContext.class, MouseEvent.class);
                drawDot.setAccessible(true);
                drawDot.invoke(app, gc, event);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            WritableImage image = new WritableImage(100, 100);
            canvas.snapshot(null, image);
            PixelReader pr = image.getPixelReader();
            drawnColor.set(pr.getColor(50, 50));
        });

        assertNotNull(drawnColor.get());
        assertEquals(1.0, drawnColor.get().getRed(), 0.01);
        assertEquals(1.0, drawnColor.get().getGreen(), 0.01);
        assertEquals(1.0, drawnColor.get().getBlue(), 0.01);
    }

    @Test
    void isCanvasBlankReturnsTrueForBlankCanvas() throws Exception {
        AtomicBoolean result = new AtomicBoolean(false);

        runOnFxThreadAndWait(() -> {
            DrawingApp app = new DrawingApp(new Stage());

            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, 100, 100);

            try {
                Method isCanvasBlank = DrawingApp.class.getDeclaredMethod("isCanvasBlank", Canvas.class);
                isCanvasBlank.setAccessible(true);
                result.set((boolean) isCanvasBlank.invoke(app, canvas));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(result.get());
    }

    @Test
    void isCanvasBlankReturnsFalseWhenCanvasHasDrawing() throws Exception {
        AtomicBoolean result = new AtomicBoolean(true);

        runOnFxThreadAndWait(() -> {
            DrawingApp app = new DrawingApp(new Stage());

            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, 100, 100);

            gc.setFill(Color.BLACK);
            gc.fillOval(40, 40, 20, 20);

            try {
                Method isCanvasBlank = DrawingApp.class.getDeclaredMethod("isCanvasBlank", Canvas.class);
                isCanvasBlank.setAccessible(true);
                result.set((boolean) isCanvasBlank.invoke(app, canvas));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertFalse(result.get());
    }
}