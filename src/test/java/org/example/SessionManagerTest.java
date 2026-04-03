package org.example;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SessionManagerTest {

    private static final String TEST_FILE = "session.txt";

    @Test
    void createsSessionFileWhenNotExists() throws Exception {
        Files.deleteIfExists(Path.of(TEST_FILE));

        Method method = SessionManager.class.getDeclaredMethod("loadOrCreateSessionId");
        method.setAccessible(true);

        String sessionId = (String) method.invoke(null);

        assertNotNull(sessionId);
        assertTrue(Files.exists(Path.of(TEST_FILE)));
    }

    @Test
    void loadsExistingSessionIdFromFile() throws Exception {
        String expectedId = "test-session-123";

        Files.writeString(Path.of(TEST_FILE), expectedId);

        Method method = SessionManager.class.getDeclaredMethod("loadOrCreateSessionId");
        method.setAccessible(true);

        String sessionId = (String) method.invoke(null);

        assertEquals(expectedId, sessionId);
    }

    @Test
    void generatesValidUUIDFormatWhenCreatingNewSession() throws Exception {
        Files.deleteIfExists(Path.of(TEST_FILE));

        Method method = SessionManager.class.getDeclaredMethod("loadOrCreateSessionId");
        method.setAccessible(true);

        String sessionId = (String) method.invoke(null);

        assertTrue(sessionId.matches("^[0-9a-fA-F\\-]{36}$"));
    }

    @Test
    void getSessionIdReturnsSameValueEveryTime() {
        String id1 = SessionManager.getSessionId();
        String id2 = SessionManager.getSessionId();

        assertEquals(id1, id2);
    }

    @Test
    void sessionFileStoresSameIdAsReturned() throws IOException {
        String id = SessionManager.getSessionId();

        String fileContent = Files.readString(Path.of(TEST_FILE)).trim();

        assertEquals(id, fileContent);
    }
}