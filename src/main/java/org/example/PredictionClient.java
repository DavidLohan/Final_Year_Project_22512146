package org.example;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    private static final String PREDICT_URL = BackendService.getBaseUrl() + "/predict";

    public static PredictionResult predict(WritableImage image) throws IOException, InterruptedException {
        byte[] imageBytes = toPngBytes(image);
        String boundary = "----JavaFXBoundary" + System.currentTimeMillis();

        byte[] body = buildMultipartBody(imageBytes, boundary);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PREDICT_URL))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
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

        String sessionPart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"session_id\"\r\n\r\n"
                + SessionManager.getSessionId() + "\r\n";

        System.out.println("Sending session: " + SessionManager.getSessionId());

        output.write(sessionPart.getBytes());
        String filePartHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"drawing.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n";

        output.write(filePartHeader.getBytes());
        output.write(imageBytes);
        output.write("\r\n".getBytes());

        String endMarker = "--" + boundary + "--\r\n";
        output.write(endMarker.getBytes());

        return output.toByteArray();
    }

    private static PredictionResult parsePrediction(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        int id = obj.get("id").getAsInt();
        String label = obj.get("label").getAsString();
        double confidence = obj.get("confidence").getAsDouble();
        String second = "";
        String third = "";
        if (obj.has("top3")) {
            JsonArray top3 = obj.getAsJsonArray("top3");
            if (top3.size() > 1) {
                second = top3.get(1).getAsJsonObject().get("label").getAsString();
            }
            if (top3.size() > 2) {
                third = top3.get(2).getAsJsonObject().get("label").getAsString();
            }
        }
        return new PredictionResult(id, label, confidence, second, third, json);
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