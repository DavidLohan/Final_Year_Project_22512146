package org.example;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PredictionClient {

    private static final String PREDICT_URL = "http://127.0.0.1:8000/predict";

    public static PredictionResult predict(WritableImage image) throws IOException, InterruptedException {
        byte[] imageBytes = toPngBytes(image);
        String boundary = "----JavaFXBoundary" + System.currentTimeMillis();

        byte[] body = buildMultipartBody(imageBytes, boundary);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PREDICT_URL))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Backend returned status " + response.statusCode() + ": " + response.body());
        }

        return parsePrediction(response.body());
    }
    private static byte[] toPngBytes(WritableImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", baos);
        return baos.toByteArray();
    }

    private static byte[] buildMultipartBody(byte[] imageBytes, String boundary) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        String start = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"drawing.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n";

        String end = "\r\n--" + boundary + "--\r\n";

        output.write(start.getBytes());
        output.write(imageBytes);
        output.write(end.getBytes());

        return output.toByteArray();
    }

    private static PredictionResult parsePrediction(String json) {
        String label = extractString(json, "\"label\"\\s*:\\s*\"([^\"]+)\"");
        double confidence = extractDouble(json, "\"confidence\"\\s*:\\s*([0-9.Ee+-]+)");

        String firstAlt = extractNthLabel(json, 2);
        String secondAlt = extractNthLabel(json, 3);

        return new PredictionResult(label, confidence, firstAlt, secondAlt, json);
    }

    private static String extractString(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private static double extractDouble(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : 0.0;
    }

    private static String extractNthLabel(String json, int n) {
        Matcher matcher = Pattern.compile("\"label\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        int count = 0;
        while (matcher.find()) {
            count++;
            if (count == n) {
                return matcher.group(1);
            }
        }
        return "";
    }
}