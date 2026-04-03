package org.example;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PredictionClientTest {

    @BeforeAll
    static void initJavaFX() {
        new JFXPanel();
    }

    @Test
    void predictReturnsPredictionResultWhenBackendReturns200() throws Exception {
        WritableImage image = new WritableImage(10, 10);

        HttpClient clientMock = mock(HttpClient.class);
        HttpClient.Builder builderMock = mock(HttpClient.Builder.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> responseMock = mock(HttpResponse.class);

        when(builderMock.version(HttpClient.Version.HTTP_1_1)).thenReturn(builderMock);
        when(builderMock.build()).thenReturn(clientMock);

        String json = """
                {
                  "id": 12,
                  "label": "cat",
                  "confidence": 0.91,
                  "top3": [
                    {"label": "cat"},
                    {"label": "dog"},
                    {"label": "bird"}
                  ]
                }
                """;

        when(responseMock.statusCode()).thenReturn(200);
        when(responseMock.body()).thenReturn(json);
        when(clientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(responseMock);

        try (MockedStatic<BackendService> backendMock = mockStatic(BackendService.class);
             MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class);
             MockedStatic<HttpClient> httpClientMock = mockStatic(HttpClient.class)) {

            backendMock.when(BackendService::getBaseUrl).thenReturn("http://localhost:8000");
            sessionMock.when(SessionManager::getSessionId).thenReturn("session-123");
            httpClientMock.when(HttpClient::newBuilder).thenReturn(builderMock);

            PredictionResult result = PredictionClient.predict(image);

            assertNotNull(result);
            assertEquals(12, result.getPredictionId());
            assertEquals(0.91, result.getConfidence(), 0.0001);
            assertEquals(3, result.getGuesses().size());
            assertEquals("cat", result.getGuesses().get(0));
            assertEquals("dog", result.getGuesses().get(1));
            assertEquals("bird", result.getGuesses().get(2));
        }
    }

    @Test
    void predictThrowsIOExceptionWhenBackendReturnsNon200() throws Exception {
        WritableImage image = new WritableImage(10, 10);

        HttpClient clientMock = mock(HttpClient.class);
        HttpClient.Builder builderMock = mock(HttpClient.Builder.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> responseMock = mock(HttpResponse.class);

        when(builderMock.version(HttpClient.Version.HTTP_1_1)).thenReturn(builderMock);
        when(builderMock.build()).thenReturn(clientMock);

        when(responseMock.statusCode()).thenReturn(500);
        when(responseMock.body()).thenReturn("Internal Server Error");
        when(clientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(responseMock);

        try (MockedStatic<BackendService> backendMock = mockStatic(BackendService.class);
             MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class);
             MockedStatic<HttpClient> httpClientMock = mockStatic(HttpClient.class)) {

            backendMock.when(BackendService::getBaseUrl).thenReturn("http://localhost:8000");
            sessionMock.when(SessionManager::getSessionId).thenReturn("session-123");
            httpClientMock.when(HttpClient::newBuilder).thenReturn(builderMock);

            IOException ex = assertThrows(IOException.class, () -> PredictionClient.predict(image));
            assertTrue(ex.getMessage().contains("Backend returned status 500"));
        }
    }

    @Test
    void parsePredictionHandlesTop3Correctly() throws Exception {
        String json = """
                {
                  "id": 7,
                  "label": "apple",
                  "confidence": 0.82,
                  "top3": [
                    {"label": "apple"},
                    {"label": "banana"},
                    {"label": "pear"}
                  ]
                }
                """;

        Method parsePrediction = PredictionClient.class.getDeclaredMethod("parsePrediction", String.class);
        parsePrediction.setAccessible(true);

        PredictionResult result = (PredictionResult) parsePrediction.invoke(null, json);

        assertEquals(7, result.getPredictionId());
        assertEquals(0.82, result.getConfidence(), 0.0001);
        assertEquals(3, result.getGuesses().size());
        assertEquals("apple", result.getGuesses().get(0));
        assertEquals("banana", result.getGuesses().get(1));
        assertEquals("pear", result.getGuesses().get(2));
    }

    @Test
    void parsePredictionWorksWhenTop3Missing() throws Exception {
        String json = """
                {
                  "id": 3,
                  "label": "house",
                  "confidence": 0.67
                }
                """;

        Method parsePrediction = PredictionClient.class.getDeclaredMethod("parsePrediction", String.class);
        parsePrediction.setAccessible(true);

        PredictionResult result = (PredictionResult) parsePrediction.invoke(null, json);

        assertEquals(3, result.getPredictionId());
        assertEquals(0.67, result.getConfidence(), 0.0001);
        assertEquals(1, result.getGuesses().size());
        assertEquals("house", result.getGuesses().get(0));
    }

    @Test
    void buildMultipartBodyIncludesSessionIdAndFilename() throws Exception {
        byte[] imageBytes = new byte[]{1, 2, 3, 4};
        String boundary = "test-boundary";

        Method buildMultipartBody = PredictionClient.class.getDeclaredMethod(
                "buildMultipartBody", byte[].class, String.class
        );
        buildMultipartBody.setAccessible(true);

        try (MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class)) {
            sessionMock.when(SessionManager::getSessionId).thenReturn("session-999");

            byte[] result = (byte[]) buildMultipartBody.invoke(null, imageBytes, boundary);
            String bodyText = new String(result);

            assertTrue(bodyText.contains("name=\"session_id\""));
            assertTrue(bodyText.contains("session-999"));
            assertTrue(bodyText.contains("name=\"file\"; filename=\"drawing.png\""));
            assertTrue(bodyText.contains("Content-Type: image/png"));
            assertTrue(bodyText.contains("--" + boundary));
        }
    }

    @Test
    void toPngBytesReturnsNonEmptyByteArray() throws Exception {
        WritableImage image = new WritableImage(5, 5);

        Method toPngBytes = PredictionClient.class.getDeclaredMethod("toPngBytes", WritableImage.class);
        toPngBytes.setAccessible(true);

        byte[] bytes = (byte[]) toPngBytes.invoke(null, image);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }
}