package com.alexi.network;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialisation de PacketCapture avec une interface par défaut (null)
        PacketCapture capture = new PacketCapture(null);

        MainMenuUI menu = new MainMenuUI(capture);
        menu.start(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}