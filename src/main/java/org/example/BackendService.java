package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.scene.image.Image;

import java.time.OffsetDateTime;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BackendService {

    private static final String BASE_URL = "http://127.0.0.1:8000";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static List<DrawingItem> fetchDrawings() throws IOException, InterruptedException {
        String sessionId = SessionManager.getSessionId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/predictions/" + sessionId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch drawings. Status: " + response.statusCode()
                    + ", Body: " + response.body());
        }

        JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonArray();
        List<DrawingItem> drawings = new ArrayList<>();

        for (JsonElement element : jsonArray) {
            JsonObject obj = element.getAsJsonObject();

            int id = obj.get("id").getAsInt();
            String label = obj.get("label").getAsString();
            String imageUrl = obj.get("imageUrl").getAsString();

            LocalDateTime createdAt = null;
            if (obj.has("createdAt") && !obj.get("createdAt").isJsonNull()) {
                String createdAtStr = obj.get("createdAt").getAsString();
                if (!createdAtStr.isBlank()) {
                    createdAt = OffsetDateTime.parse(createdAtStr).toLocalDateTime();
                }
            }

            Image image = new Image(imageUrl, true);
            drawings.add(new DrawingItem(id, label, image, createdAt));
        }

        return drawings;
    }

    public static void updateDrawingLabel(int id, String newLabel) throws IOException, InterruptedException {
        String json = "{\"new_label\":\"" + newLabel + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/predictions/" + id + "/label"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to update drawing label. Status: " + response.statusCode()
                    + ", Body: " + response.body());
        }
    }

    public static void deleteDrawing(int id) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/predictions/" + id))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to delete drawing. Status: " + response.statusCode()
                    + ", Body: " + response.body());
        }
    }

    public static void deleteAllDrawingsForSession(String sessionId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/predictions/session/" + sessionId))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to clear drawings. Status: " + response.statusCode()
                    + ", Body: " + response.body());
        }
    }
}