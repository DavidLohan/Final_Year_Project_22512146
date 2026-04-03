package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BackendServiceTest {

    @Test
    void getBaseUrlReturnsDefaultOrConfiguredValue() {
        String url = BackendService.getBaseUrl();
        assertNotNull(url);
        assertTrue(url.startsWith("http"));
    }

    @Test
    void healthCheckReturnsFalseWhenBackendIsUnavailable() {
        boolean result = BackendService.healthCheck();
        assertFalse(result);
    }

    @Test
    void updateDrawingLabelThrowsExceptionWhenBackendUnavailable() {
        assertThrows(Exception.class,
                () -> BackendService.updateDrawingLabel(1, "new"));
    }

    @Test
    void deleteDrawingThrowsExceptionWhenBackendUnavailable() {
        assertThrows(Exception.class,
                () -> BackendService.deleteDrawing(1));
    }

    @Test
    void deleteAllDrawingsForSessionThrowsExceptionWhenBackendUnavailable() {
        assertThrows(Exception.class,
                () -> BackendService.deleteAllDrawingsForSession("session"));
    }
}