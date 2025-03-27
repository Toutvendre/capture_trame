package com.alexi.network;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import org.pcap4j.core.PcapNativeException;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.scene.control.cell.PropertyValueFactory;

public class PacketCaptureUI {

    private TableView<PacketInfo> table;
    private PacketCapture capture;
    private Label statusLabel;
    private MainMenuUI mainMenuUI; // Référence à l’instance de MainMenuUI

    public PacketCaptureUI(PacketCapture capture, MainMenuUI mainMenuUI) {
        this.capture = capture;
        this.mainMenuUI = mainMenuUI;
        this.capture.setUI(this);
    }

    @SuppressWarnings("unchecked")
    public void start(Stage stage) throws PcapNativeException {
        // En-tête avec le titre
        Label title = new Label("Capture de Paquets");
        title.getStyleClass().add("label");
        HBox header = new HBox(title);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20));
        header.getStyleClass().add("header");

        // Barre d’outils pour les boutons d’action
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setPadding(new Insets(10));
        toolbar.getStyleClass().add("toolbar");

        Button startButton = new Button("Démarrer Capture");
        Button stopButton = new Button("Arrêter Capture");
        Button backButton = new Button("Retour");
        stopButton.setDisable(true);

        for (Button btn : new Button[] { startButton, stopButton, backButton }) {
            btn.getStyleClass().add("button");
            btn.setPrefWidth(220);
            btn.setMinHeight(70);
            // Animation d’apparition pour chaque bouton
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), btn);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        }

        toolbar.getChildren().addAll(startButton, stopButton, backButton);

        // Label de statut
        statusLabel = new Label("Prêt à capturer...");
        statusLabel.getStyleClass().add("label");

        // TableView pour afficher les paquets
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<PacketInfo, String> timestampCol = new TableColumn<>("Date et Heure");
        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        TableColumn<PacketInfo, String> srcIpCol = new TableColumn<>("IP Source");
        srcIpCol.setCellValueFactory(new PropertyValueFactory<>("srcIp"));

        TableColumn<PacketInfo, String> dstIpCol = new TableColumn<>("IP Destination");
        dstIpCol.setCellValueFactory(new PropertyValueFactory<>("dstIp"));

        TableColumn<PacketInfo, String> srcPortCol = new TableColumn<>("Port Source");
        srcPortCol.setCellValueFactory(new PropertyValueFactory<>("srcPort"));

        TableColumn<PacketInfo, String> dstPortCol = new TableColumn<>("Port Destination");
        dstPortCol.setCellValueFactory(new PropertyValueFactory<>("dstPort"));

        TableColumn<PacketInfo, String> protocolCol = new TableColumn<>("Protocole");
        protocolCol.setCellValueFactory(new PropertyValueFactory<>("protocol"));

        table.getColumns().addAll(timestampCol, srcIpCol, dstIpCol, srcPortCol, dstPortCol, protocolCol);

        // Colorer les lignes selon le protocole (comme dans MainMenuUI)
        table.setRowFactory(tv -> new TableRow<PacketInfo>() {
            @Override
            protected void updateItem(PacketInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    if ("TCP".equals(item.getProtocol())) {
                        setStyle("-fx-background-color: #2a3442;");
                    } else if ("UDP".equals(item.getProtocol())) {
                        setStyle("-fx-background-color: #363d47;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Animation d’apparition pour le tableau
        FadeTransition fadeInTable = new FadeTransition(Duration.millis(500), table);
        fadeInTable.setFromValue(0);
        fadeInTable.setToValue(1);
        fadeInTable.play();

        // Layout principal
        VBox mainLayout = new VBox(20, header, toolbar, statusLabel, table);
        mainLayout.getStyleClass().add("root");
        mainLayout.setAlignment(Pos.CENTER);
        VBox.setVgrow(table, Priority.ALWAYS); // Le tableau s’étend pour remplir l’espace

        // Animation d’apparition pour le layout principal
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), mainLayout);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Actions des boutons
        startButton.setOnAction(e -> {
            try {
                capture.startCapture();
                startButton.setDisable(true);
                stopButton.setDisable(false);
                statusLabel.setText("Capture en cours...");
            } catch (PcapNativeException ex) {
                showError("Erreur au démarrage : " + ex.getMessage());
            }
        });

        stopButton.setOnAction(e -> {
            capture.stopCapture();
            startButton.setDisable(false);
            stopButton.setDisable(true);
            captureStopped();
        });

        backButton.setOnAction(e -> {
            capture.stopCapture();
            mainMenuUI.show(); // Réafficher l’instance existante
            stage.close();
        });

        Scene scene = new Scene(mainLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("Filtrage - Analyseur de Trafic Réseau");
        stage.setScene(scene);
        stage.show();
    }

    public void addPacketToTable(PacketInfo info) {
        table.getItems().add(info);
        System.out.println("[INFO] Paquet ajouté à la table : " + info);
    }

    public void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.RED);
    }

    public void captureStopped() {
        statusLabel.setText("Capture arrêtée.");
        statusLabel.setTextFill(Color.DARKRED);
    }
}