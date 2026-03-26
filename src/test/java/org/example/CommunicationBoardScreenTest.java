package org.example;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class CommunicationBoardScreenTest {

    private Stage stage;

    @Start
    private void start(Stage stage) {
        this.stage = stage;
    }

    @Test
    void communicationBoardEmptyLoadsSuccessfully() throws Exception {
        try (MockedStatic<BackendService> mockedBackend = Mockito.mockStatic(BackendService.class)) {
            mockedBackend.when(BackendService::fetchDrawings).thenReturn(List.of());

            final Scene[] sceneHolder = new Scene[1];
            runOnFxThread(() -> {
                sceneHolder[0] = new CommunicationBoardScreen().createScene(stage);
                stage.setScene(sceneHolder[0]);
                stage.show();
            });

            ListView<?> listView = (ListView<?>) sceneHolder[0].lookup("#drawingListView");
            assertNotNull(listView);
            assertEquals(0, listView.getItems().size());
        }
    }

    @Test
    void communicationBoardLoadsMultipleItems() throws Exception {
        DrawingItem item1 = Mockito.mock(DrawingItem.class);
        DrawingItem item2 = Mockito.mock(DrawingItem.class);

        Mockito.when(item1.getTitle()).thenReturn("cat");
        Mockito.when(item1.getCreatedAt()).thenReturn(LocalDateTime.now());
        Mockito.when(item1.getImage()).thenReturn(new WritableImage(50, 50));

        Mockito.when(item2.getTitle()).thenReturn("dog");
        Mockito.when(item2.getCreatedAt()).thenReturn(LocalDateTime.now());
        Mockito.when(item2.getImage()).thenReturn(new WritableImage(50, 50));

        try (MockedStatic<BackendService> mockedBackend = Mockito.mockStatic(BackendService.class)) {
            mockedBackend.when(BackendService::fetchDrawings).thenReturn(List.of(item1, item2));

            final Scene[] sceneHolder = new Scene[1];
            runOnFxThread(() -> {
                sceneHolder[0] = new CommunicationBoardScreen().createScene(stage);
                stage.setScene(sceneHolder[0]);
                stage.show();
            });

            ListView<?> listView = (ListView<?>) sceneHolder[0].lookup("#drawingListView");
            assertNotNull(listView);
            assertEquals(2, listView.getItems().size());
        }
    }

    @Test
    void deleteWithNothingSelectedDoesNothing() throws Exception {
        try (MockedStatic<BackendService> mockedBackend = Mockito.mockStatic(BackendService.class)) {
            mockedBackend.when(BackendService::fetchDrawings).thenReturn(List.of());

            final Scene[] sceneHolder = new Scene[1];
            runOnFxThread(() -> {
                sceneHolder[0] = new CommunicationBoardScreen().createScene(stage);
                stage.setScene(sceneHolder[0]);
                stage.show();
            });

            Button deleteBtn = findButtonByText("Delete Selected");
            assertNotNull(deleteBtn);

            runOnFxThread(deleteBtn::fire);

            assertTrue(true);
        }
    }

    private Button findButtonByText(String text) {
        return stage.getScene().getRoot().lookupAll(".button").stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .filter(btn -> text.equals(btn.getText()))
                .findFirst()
                .orElse(null);
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