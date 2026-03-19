package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class SessionManager {

    private static final String SESSION_FILE = "session.txt";
    private static final String SESSION_ID = loadOrCreateSessionId();

    private static String loadOrCreateSessionId() {
        Path path = Path.of(SESSION_FILE);

        try {
            if (Files.exists(path)) {
                return Files.readString(path).trim();
            }

            String newSessionId = UUID.randomUUID().toString();
            Files.writeString(path, newSessionId);
            return newSessionId;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load or create session ID", e);
        }
    }

    public static String getSessionId() {
        return SESSION_ID;
    }
}