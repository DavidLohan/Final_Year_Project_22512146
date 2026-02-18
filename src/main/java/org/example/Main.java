package org.example;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Final Year Project");
        stage.setScene(new HomeScreen(stage).getScene());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
