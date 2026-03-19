package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class DrawingApp {

    private final Scene scene;

    private boolean eraserOn = false;
    private double brushSize = 6.0;
    private Color brushColor = Color.BLACK;

    public DrawingApp(Stage stage) {

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        Label title = new Label("Drawing Canvas");
        title.getStyleClass().add("title-label");

        Canvas canvas = new Canvas(900, 560);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        clearCanvas(gc, canvas);

        Button backBtn = new Button("Back");
        ToggleButton eraserBtn = new ToggleButton("Eraser");
        Button clearBtn = new Button("Clear");
        Button submitBtn = new Button("Submit");

        ColorPicker colorPicker = new ColorPicker(brushColor);

        Slider sizeSlider = new Slider(1, 40, brushSize);
        sizeSlider.setShowTickLabels(true);
        sizeSlider.setShowTickMarks(true);
        sizeSlider.setPrefWidth(180);

        Label feedbackLabel = new Label("Draw something, then press Submit.");
        feedbackLabel.getStyleClass().add("info-label");
        feedbackLabel.setWrapText(true);

        backBtn.getStyleClass().add("secondary-button");
        eraserBtn.getStyleClass().add("secondary-button");
        clearBtn.getStyleClass().add("secondary-button");
        submitBtn.getStyleClass().add("primary-button");

        HBox controls = new HBox(
                12,
                backBtn,
                new Separator(),
                new Label("Color:"),
                colorPicker,
                new Label("Brush:"),
                sizeSlider,
                eraserBtn,
                clearBtn,
                submitBtn
        );
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox toolbarBox = new VBox(12, title, controls);
        toolbarBox.getStyleClass().add("card");

        StackPane canvasHolder = new StackPane(canvas);
        canvasHolder.setAlignment(Pos.CENTER);
        canvasHolder.getStyleClass().add("canvas-box");

        VBox bottomBox = new VBox(feedbackLabel);
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        bottomBox.getStyleClass().add("card");

        root.setTop(toolbarBox);
        root.setCenter(canvasHolder);
        root.setBottom(bottomBox);

        BorderPane.setMargin(toolbarBox, new Insets(0, 0, 20, 0));
        BorderPane.setMargin(canvasHolder, new Insets(0, 0, 20, 0));

        scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(
                getClass().getResource("/design/style.css").toExternalForm()
        );

        backBtn.setOnAction(e -> stage.setScene(new HomeScreen(stage).getScene()));

        colorPicker.setOnAction(e -> brushColor = colorPicker.getValue());
        sizeSlider.valueProperty().addListener((obs, oldV, newV) -> brushSize = newV.doubleValue());
        eraserBtn.setOnAction(e -> eraserOn = eraserBtn.isSelected());

        clearBtn.setOnAction(e -> {
            clearCanvas(gc, canvas);
            feedbackLabel.setText("Canvas cleared.");
        });

        submitBtn.setOnAction(e -> {
            if (isCanvasBlank(canvas)) {
                feedbackLabel.setText("Nothing to submit — please draw something first.");
                return;
            }

            WritableImage snapshot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
            canvas.snapshot(null, snapshot);

            try {
                PredictionResult result = PredictionClient.predict(snapshot);
                stage.setScene(new FeedbackScreen().createScene(stage, snapshot, result));
            } catch (Exception ex) {
                feedbackLabel.setText("Prediction failed: " + ex.getMessage());
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> drawDot(gc, e));
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> drawDot(gc, e));
    }

    private void clearCanvas(GraphicsContext gc, Canvas canvas) {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void drawDot(GraphicsContext gc, MouseEvent e) {
        gc.setFill(eraserOn ? Color.WHITE : brushColor);
        double x = e.getX() - brushSize / 2;
        double y = e.getY() - brushSize / 2;
        gc.fillOval(x, y, brushSize, brushSize);
    }

    private boolean isCanvasBlank(Canvas canvas) {
        int w = (int) canvas.getWidth();
        int h = (int) canvas.getHeight();

        WritableImage snapshot = new WritableImage(w, h);
        canvas.snapshot(null, snapshot);

        PixelReader pr = snapshot.getPixelReader();
        if (pr == null) return true;

        final double tol = 0.03;
        final int step = 2;

        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                Color c = pr.getColor(x, y);

                if (c.getOpacity() > 0.01 &&
                        (c.getRed() < 1.0 - tol || c.getGreen() < 1.0 - tol || c.getBlue() < 1.0 - tol)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Scene getScene() {
        return scene;
    }
}