package com.alexi.network;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*; // Importation pour HBox, VBox, GridPane
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainMenuUI {

    private PacketCapture capture;
    private Label statusLabel;
    private boolean isExpertMode = false;
    private boolean isStealthMode = false;
    private Stage primaryStage;
    private List<PcapNetworkInterface> detectedInterfaces;
    private Map<String, Integer> protocolStats; // Statistiques des protocoles

    public MainMenuUI(PacketCapture capture) {
        this.capture = capture;
        this.detectedInterfaces = new ArrayList<>();
        this.protocolStats = new HashMap<>();
    }

    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // En-tête avec le titre
        statusLabel = new Label("Analyseur de Trafic Réseau");
        statusLabel.getStyleClass().add("label");
        HBox header = new HBox(statusLabel);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20));
        header.getStyleClass().add("header");

        // Section principale pour les actions
        VBox actionSection = new VBox(20);
        actionSection.setAlignment(Pos.CENTER);
        actionSection.setPadding(new Insets(30));
        actionSection.getStyleClass().add("action-section");

        // Boutons principaux (disposition en grille dans la section)
        GridPane actionGrid = new GridPane();
        actionGrid.setHgap(20);
        actionGrid.setVgap(20);
        actionGrid.setAlignment(Pos.CENTER);

        Button detectButton = createButton("Détecter Interfaces", "/icons/detect.png");
        Button selectButton = createButton("Sélectionner Interface", "/icons/select.png");
        selectButton.setDisable(true); // Désactivé jusqu'à la détection
        Button filterButton = createButton("Configurer Filtres", "/icons/filter.png");
        Button captureButton = createButton("Lancer Capture", "/icons/capture.png");
        Button analyzeButton = createButton("Analyser Paquets", "/icons/analyze.png");
        Button exportButton = createButton("Exporter Données", "/icons/export.png");

        detectButton.setOnAction(e -> openDetectInterfaces());
        selectButton.setOnAction(e -> openSelectInterface());
        filterButton.setOnAction(e -> openConfigureFilters());
        // Correction : Ajout du try-catch pour gérer PcapNativeException
        captureButton.setOnAction(e -> {
            try {
                openCaptureWindow(primaryStage);
            } catch (PcapNativeException ex) {
                statusLabel.setText("Erreur lors du lancement de la capture : " + ex.getMessage());
                System.out.println("[ERROR] Erreur lors du lancement de la capture : " + ex.getMessage());
            }
        });
        analyzeButton.setOnAction(e -> openAnalyzePackets());
        exportButton.setOnAction(e -> openExportData());

        actionGrid.add(detectButton, 0, 0);
        actionGrid.add(selectButton, 1, 0);
        actionGrid.add(filterButton, 2, 0);
        actionGrid.add(captureButton, 0, 1);
        actionGrid.add(analyzeButton, 1, 1);
        actionGrid.add(exportButton, 2, 1);

        actionSection.getChildren().add(actionGrid);

        // Barre de navigation en bas pour les options secondaires
        HBox navBar = new HBox(20);
        navBar.setAlignment(Pos.CENTER);
        navBar.setPadding(new Insets(20));
        navBar.getStyleClass().add("nav-bar");

        Button modeButton = createButton("Mode Simple", "/icons/mode.png");
        Button statsButton = createButton("Afficher Stats", "/icons/stats.png");
        Button stealthButton = createButton("Activer Mode Furtif", "/icons/stealth.png");
        Button quitButton = createButton("Quitter", "/icons/quit.png");

        modeButton.setOnAction(e -> {
            toggleMode();
            modeButton.setText(isExpertMode ? "Mode Expert" : "Mode Simple");
        });
        statsButton.setOnAction(e -> openShowStats());
        stealthButton.setOnAction(e -> {
            enableStealthMode();
            stealthButton.setText(isStealthMode ? "Désactiver Mode Furtif" : "Activer Mode Furtif");
        });
        quitButton.setOnAction(e -> primaryStage.close());

        navBar.getChildren().addAll(modeButton, statsButton, stealthButton, quitButton);

        // Layout principal
        VBox mainLayout = new VBox(20, header, actionSection, navBar);
        mainLayout.getStyleClass().add("root");
        mainLayout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(mainLayout, 900, 650);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        // Animation d’apparition pour le layout principal
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), mainLayout);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        primaryStage.setTitle("Menu Principal - Analyseur de Trafic Réseau");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(550);
        primaryStage.show();
    }

    public void show() {
        primaryStage.show();
    }

    private Button createButton(String text, String iconPath) {
        Button btn = new Button(text);
        try {
            Image icon = new Image(getClass().getResourceAsStream(iconPath));
            ImageView iconView = new ImageView(icon);
            iconView.setFitHeight(40);
            iconView.setFitWidth(40);
            btn.setGraphic(iconView);
        } catch (Exception e) {
            System.out.println("Erreur lors du chargement de l’icône : " + iconPath);
        }
        btn.getStyleClass().add("button");
        btn.setPrefWidth(220);
        btn.setMinHeight(70);
        btn.setAlignment(Pos.CENTER_LEFT);

        // Animation d’apparition pour chaque bouton
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), btn);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        return btn;
    }

    private void openDetectInterfaces() {
        statusLabel.setText("Détection en cours...");
        Stage detectionStage = new Stage();
        detectionStage.initModality(Modality.APPLICATION_MODAL);
        detectionStage.setTitle("Détection des Interfaces");

        VBox detectionLayout = new VBox(20);
        detectionLayout.setPadding(new Insets(30));
        detectionLayout.setAlignment(Pos.CENTER);
        detectionLayout.getStyleClass().add("root");

        Label detectionLabel = new Label("Détection en cours...");
        detectionLabel.getStyleClass().add("label");

        detectionLayout.getChildren().add(detectionLabel);
        Scene detectionScene = new Scene(detectionLayout, 400, 200);
        detectionScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        detectionStage.setScene(detectionScene);
        detectionStage.show();

        new Thread(() -> {
            try {
                detectedInterfaces = Pcaps.findAllDevs();
                Platform.runLater(() -> {
                    if (detectedInterfaces.isEmpty()) {
                        detectionLabel.setText("Aucune interface détectée.");
                        statusLabel.setText("Aucune interface détectée.");
                    } else {
                        detectionLabel
                                .setText("Détection terminée. " + detectedInterfaces.size() + " interfaces trouvées.");
                        statusLabel
                                .setText("Détection terminée. " + detectedInterfaces.size() + " interfaces trouvées.");
                    }
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(500), detectionLayout);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(e -> detectionStage.close());
                    fadeOut.play();
                });
            } catch (PcapNativeException e) {
                Platform.runLater(() -> {
                    detectionLabel.setText("Erreur : " + e.getMessage());
                    statusLabel.setText("Erreur lors de la détection : " + e.getMessage());
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(500), detectionLayout);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(event -> detectionStage.close());
                    fadeOut.play();
                });
            }
        }).start();
    }

    private void openSelectInterface() {
        statusLabel.setText("Sélection d’interface...");
        if (detectedInterfaces.isEmpty()) {
            statusLabel.setText("Aucune interface détectée. Veuillez d’abord lancer la détection.");
            return;
        }

        Stage selectStage = new Stage();
        selectStage.initModality(Modality.APPLICATION_MODAL);
        VBox selectLayout = new VBox(20);
        selectLayout.setPadding(new Insets(30));
        selectLayout.setAlignment(Pos.CENTER);
        selectLayout.getStyleClass().add("root");

        Label title = new Label("Choisissez une interface réseau");
        title.getStyleClass().add("label");

        ComboBox<String> interfaceCombo = new ComboBox<>();
        for (int i = 0; i < detectedInterfaces.size(); i++) {
            PcapNetworkInterface dev = detectedInterfaces.get(i);
            String name = dev.getName();
            String desc = dev.getDescription() != null ? dev.getDescription() : "Sans description";
            String displayName;

            if (name.toLowerCase().contains("wlan") || desc.toLowerCase().contains("wireless")
                    || desc.toLowerCase().contains("wifi")) {
                displayName = "WiFi (" + name + ")";
            } else if (name.toLowerCase().contains("eth") || desc.toLowerCase().contains("ethernet")) {
                displayName = "Ethernet (" + name + ")";
            } else {
                displayName = "Interface " + (i + 1) + " (" + name + ")";
            }

            interfaceCombo.getItems().add(i + ": " + displayName + " - Actif : " + dev.isUp());
        }
        interfaceCombo.getSelectionModel().selectFirst();
        interfaceCombo.setPrefWidth(300);
        interfaceCombo.getStyleClass().add("combo-box");

        Button confirmButton = createButton("Confirmer", "/icons/confirm.png");
        confirmButton.setOnAction(e -> {
            int selectedIndex = interfaceCombo.getSelectionModel().getSelectedIndex();
            PcapNetworkInterface selectedInterface = detectedInterfaces.get(selectedIndex);
            capture.setSelectedInterface(selectedInterface);

            String name = selectedInterface.getName();
            String desc = selectedInterface.getDescription() != null ? selectedInterface.getDescription()
                    : "Sans description";
            String displayName;
            if (name.toLowerCase().contains("wlan") || desc.toLowerCase().contains("wireless")
                    || desc.toLowerCase().contains("wifi")) {
                displayName = "WiFi sélectionné";
            } else if (name.toLowerCase().contains("eth") || desc.toLowerCase().contains("ethernet")) {
                displayName = "Ethernet sélectionné";
            } else {
                displayName = "Interface sélectionnée";
            }
            statusLabel.setText("Interface sélectionnée : " + displayName);
            selectStage.close();
        });

        selectLayout.getChildren().addAll(title, interfaceCombo, confirmButton);
        Scene selectScene = new Scene(selectLayout, 500, 300);
        selectScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        selectStage.setScene(selectScene);
        selectStage.setTitle("Sélectionner une Interface");
        selectStage.show();
    }

    private void openConfigureFilters() {
        Stage filterStage = new Stage();
        filterStage.initModality(Modality.APPLICATION_MODAL);
        VBox filterLayout = new VBox(20);
        filterLayout.setPadding(new Insets(30));
        filterLayout.setAlignment(Pos.CENTER);
        filterLayout.getStyleClass().add("root");

        Label title = new Label("Configurer les filtres");
        title.getStyleClass().add("label");

        ComboBox<String> protocolCombo = new ComboBox<>();
        protocolCombo.getItems().addAll("tcp", "udp", "tcp et udp");
        protocolCombo.getSelectionModel().select("tcp et udp");
        protocolCombo.setPrefWidth(300);
        protocolCombo.getStyleClass().add("combo-box");

        Label srcPortLabel = new Label("Port source (optionnel) :");
        srcPortLabel.getStyleClass().add("label");
        TextField srcPortField = new TextField();
        srcPortField.setPromptText("Ex. 80");
        srcPortField.setPrefWidth(300);
        srcPortField.getStyleClass().add("text-field");

        Label dstPortLabel = new Label("Port destination (optionnel) :");
        dstPortLabel.getStyleClass().add("label");
        TextField dstPortField = new TextField();
        dstPortField.setPromptText("Ex. 443");
        dstPortField.setPrefWidth(300);
        dstPortField.getStyleClass().add("text-field");

        Button applyButton = createButton("Appliquer", "/icons/confirm.png");
        applyButton.setOnAction(e -> {
            String protocol = protocolCombo.getValue().replace("tcp et udp", "tcp or udp");
            String srcPort = srcPortField.getText().trim();
            String dstPort = dstPortField.getText().trim();
            StringBuilder filter = new StringBuilder();
            if (protocol != null && !protocol.isEmpty()) {
                filter.append(protocol);
            }
            if (!srcPort.isEmpty()) {
                try {
                    Integer.parseInt(srcPort);
                    if (filter.length() > 0)
                        filter.append(" and ");
                    filter.append("src port ").append(srcPort);
                } catch (NumberFormatException ex) {
                    statusLabel.setText("Erreur : Port source invalide.");
                    return;
                }
            }
            if (!dstPort.isEmpty()) {
                try {
                    Integer.parseInt(dstPort);
                    if (filter.length() > 0)
                        filter.append(" and ");
                    filter.append("dst port ").append(dstPort);
                } catch (NumberFormatException ex) {
                    statusLabel.setText("Erreur : Port destination invalide.");
                    return;
                }
            }
            String finalFilter = filter.toString();
            if (finalFilter.isEmpty())
                finalFilter = "tcp or udp";
            capture.setFilter(finalFilter);
            statusLabel.setText("Filtre appliqué : " + finalFilter);
            filterStage.close();
        });

        filterLayout.getChildren().addAll(title, protocolCombo, srcPortLabel, srcPortField, dstPortLabel, dstPortField,
                applyButton);
        Scene filterScene = new Scene(filterLayout, 400, 350);
        filterScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        filterStage.setScene(filterScene);
        filterStage.setTitle("Configurer Filtres");
        filterStage.show();
    }

    private void openCaptureWindow(Stage primaryStage) throws PcapNativeException {
        Stage captureStage = new Stage();
        PacketCaptureUI captureUI = new PacketCaptureUI(capture, this);
        captureUI.start(captureStage);
        captureStage.setOnCloseRequest(e -> {
            capture.stopCapture();
            primaryStage.show();
        });
        primaryStage.hide();
    }

    @SuppressWarnings("unchecked")
    private void openAnalyzePackets() {
        Stage analyzeStage = new Stage();
        VBox analyzeLayout = new VBox(20);
        analyzeLayout.setPadding(new Insets(30));
        analyzeLayout.getStyleClass().add("root");

        // En-tête avec le titre
        Label title = new Label("Analyse des paquets capturés");
        title.getStyleClass().add("label");
        HBox header = new HBox(title);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20));
        header.getStyleClass().add("header");

        // Barre d’outils pour le filtrage
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10));
        toolbar.getStyleClass().add("toolbar");

        Label protocolLabel = new Label("Filtrer par protocole :");
        protocolLabel.getStyleClass().add("label");
        ComboBox<String> protocolFilter = new ComboBox<>();
        protocolFilter.getItems().addAll("Tous", "TCP", "UDP", "ICMP");
        protocolFilter.getSelectionModel().select("Tous");
        protocolFilter.getStyleClass().add("combo-box");

        Label ipLabel = new Label("IP (Source ou Dest.) :");
        ipLabel.getStyleClass().add("label");
        TextField ipFilter = new TextField();
        ipFilter.setPromptText("Ex: 192.168.1.1");
        ipFilter.getStyleClass().add("text-field");

        // Ajout d’un filtre par port (Expert Mode uniquement)
        Label portLabel = new Label("Port (Source ou Dest.) :");
        portLabel.getStyleClass().add("label");
        TextField portFilter = new TextField();
        portFilter.setPromptText("Ex: 80");
        portFilter.getStyleClass().add("text-field");
        portLabel.setVisible(isExpertMode);
        portFilter.setVisible(isExpertMode);

        Button applyFilterButton = new Button("Appliquer Filtre");
        applyFilterButton.getStyleClass().add("button");
        applyFilterButton.setPrefWidth(150);

        Button clearFilterButton = new Button("Effacer Filtres");
        clearFilterButton.getStyleClass().add("button");
        clearFilterButton.setPrefWidth(150);

        toolbar.getChildren().addAll(protocolLabel, protocolFilter, ipLabel, ipFilter);
        if (isExpertMode) {
            toolbar.getChildren().addAll(portLabel, portFilter);
        }
        toolbar.getChildren().addAll(applyFilterButton, clearFilterButton);

        // Explications des champs
        Label infoLabel = new Label("Voici les paquets capturés. Double-cliquez sur un paquet pour plus de détails.\n" +
                "- Date et Heure : Quand le paquet a été capturé\n" +
                "- IP Source : Adresse de l’appareil qui envoie\n" +
                "- IP Destination : Adresse de l’appareil qui reçoit\n" +
                (isExpertMode ? "- Port Source : Point de départ de la communication\n" +
                        "- Port Destination : Point d’arrivée de la communication\n" : "")
                +
                "- Protocole : Type de communication (TCP = fiable, UDP = rapide)");
        infoLabel.getStyleClass().add("label");
        infoLabel.setWrapText(true);

        // TableView pour afficher les paquets
        TableView<PacketInfo> packetTable = new TableView<>();
        packetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        packetTable.setPrefHeight(400);

        TableColumn<PacketInfo, String> timestampCol = new TableColumn<>("Date et Heure");
        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        Tooltip timestampTooltip = new Tooltip("Date et heure auxquelles le paquet a été capturé.");
        timestampTooltip.getStyleClass().add("tooltip");
        timestampCol.setGraphic(new Label("Date et Heure") {
            {
                setTooltip(timestampTooltip);
            }
        });

        TableColumn<PacketInfo, String> srcIpCol = new TableColumn<>("IP Source");
        srcIpCol.setCellValueFactory(new PropertyValueFactory<>("srcIp"));
        Tooltip srcIpTooltip = new Tooltip("Adresse de l’appareil qui envoie le paquet.");
        srcIpTooltip.getStyleClass().add("tooltip");
        srcIpCol.setGraphic(new Label("IP Source") {
            {
                setTooltip(srcIpTooltip);
            }
        });

        TableColumn<PacketInfo, String> dstIpCol = new TableColumn<>("IP Destination");
        dstIpCol.setCellValueFactory(new PropertyValueFactory<>("dstIp"));
        Tooltip dstIpTooltip = new Tooltip("Adresse de l’appareil qui reçoit le paquet.");
        dstIpTooltip.getStyleClass().add("tooltip");
        dstIpCol.setGraphic(new Label("IP Destination") {
            {
                setTooltip(dstIpTooltip);
            }
        });

        TableColumn<PacketInfo, String> srcPortCol = new TableColumn<>("Port Source");
        srcPortCol.setCellValueFactory(new PropertyValueFactory<>("srcPort"));
        srcPortCol.setVisible(isExpertMode);
        Tooltip srcPortTooltip = new Tooltip("Point de départ de la communication sur l’appareil source.");
        srcPortTooltip.getStyleClass().add("tooltip");
        srcPortCol.setGraphic(new Label("Port Source") {
            {
                setTooltip(srcPortTooltip);
            }
        });

        TableColumn<PacketInfo, String> dstPortCol = new TableColumn<>("Port Destination");
        dstPortCol.setCellValueFactory(new PropertyValueFactory<>("dstPort"));
        dstPortCol.setVisible(isExpertMode);
        Tooltip dstPortTooltip = new Tooltip("Point d’arrivée de la communication sur l’appareil destination.");
        dstPortTooltip.getStyleClass().add("tooltip");
        dstPortCol.setGraphic(new Label("Port Destination") {
            {
                setTooltip(dstPortTooltip);
            }
        });

        TableColumn<PacketInfo, String> protocolCol = new TableColumn<>("Protocole");
        protocolCol.setCellValueFactory(new PropertyValueFactory<>("protocol"));
        Tooltip protocolTooltip = new Tooltip("Type de communication (TCP = fiable, UDP = rapide).");
        protocolTooltip.getStyleClass().add("tooltip");
        protocolCol.setGraphic(new Label("Protocole") {
            {
                setTooltip(protocolTooltip);
            }
        });

        packetTable.getColumns().addAll(timestampCol, srcIpCol, dstIpCol, srcPortCol, dstPortCol, protocolCol);

        // Colorer les lignes selon le protocole
        packetTable.setRowFactory(tv -> new TableRow<PacketInfo>() {
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

        // Charger les paquets initiaux et mettre à jour les statistiques
        List<PacketInfo> packets = capture.getCapturedPackets();
        System.out.println("[DEBUG] Nombre de paquets capturés dans openAnalyzePackets : " + packets.size());
        for (PacketInfo packet : packets) {
            System.out.println("[DEBUG] Paquet : " + packet);
            protocolStats.put(packet.getProtocol(), protocolStats.getOrDefault(packet.getProtocol(), 0) + 1);
        }

        packetTable.getItems().clear();
        if (packets.isEmpty()) {
            packetTable.setPlaceholder(new Label("Aucun paquet capturé. Essayez de lancer une capture."));
        } else {
            packetTable.getItems().addAll(packets);
            packetTable.refresh();
        }

        // Action du bouton "Appliquer Filtre"
        applyFilterButton.setOnAction(e -> {
            String selectedProtocol = protocolFilter.getValue();
            String ipFilterText = ipFilter.getText().trim();
            String portFilterText = portFilter.getText().trim();

            packetTable.getItems().clear();
            for (PacketInfo packet : capture.getCapturedPackets()) {
                boolean matchesProtocol = "Tous".equals(selectedProtocol)
                        || packet.getProtocol().equals(selectedProtocol);
                boolean matchesIp = ipFilterText.isEmpty() ||
                        packet.getSrcIp().contains(ipFilterText) ||
                        packet.getDstIp().contains(ipFilterText);
                boolean matchesPort = !isExpertMode || portFilterText.isEmpty() ||
                        packet.getSrcPort().contains(portFilterText) ||
                        packet.getDstPort().contains(portFilterText);
                if (matchesProtocol && matchesIp && matchesPort) {
                    packetTable.getItems().add(packet);
                }
            }
            packetTable.refresh();
            System.out.println("[DEBUG] Paquets après filtrage (Protocole: " + selectedProtocol + ", IP: "
                    + ipFilterText + ", Port: " + portFilterText + ") : " + packetTable.getItems().size());
        });

        // Action du bouton "Effacer Filtres"
        clearFilterButton.setOnAction(e -> {
            protocolFilter.getSelectionModel().select("Tous");
            ipFilter.clear();
            portFilter.clear();
            packetTable.getItems().clear();
            packetTable.getItems().addAll(capture.getCapturedPackets());
            packetTable.refresh();
            System.out.println("[DEBUG] Filtres effacés. Paquets affichés : " + packetTable.getItems().size());
        });

        // Afficher les détails d’un paquet lors d’un double-clic
        packetTable.setOnMouseClicked(event -> {
            PacketInfo selectedPacket = packetTable.getSelectionModel().getSelectedItem();
            if (selectedPacket != null && event.getClickCount() == 2) {
                Stage detailStage = new Stage();
                VBox detailLayout = new VBox(10);
                detailLayout.setPadding(new Insets(20));
                detailLayout.getStyleClass().add("root");

                Label detailTitle = new Label("Détails du paquet");
                detailTitle.getStyleClass().add("label");

                StringBuilder detailsText = new StringBuilder();
                detailsText.append("Date et Heure : ").append(selectedPacket.getTimestamp()).append("\n")
                        .append("Appareil qui envoie (IP Source) : ").append(selectedPacket.getSrcIp()).append("\n")
                        .append("Appareil qui reçoit (IP Destination) : ").append(selectedPacket.getDstIp())
                        .append("\n");
                if (isExpertMode) {
                    detailsText.append("Port de départ (Port Source) : ").append(selectedPacket.getSrcPort())
                            .append("\n")
                            .append("Port d’arrivée (Port Destination) : ").append(selectedPacket.getDstPort())
                            .append("\n");
                }
                detailsText.append("Type de communication (Protocole) : ").append(selectedPacket.getProtocol())
                        .append("\n");
                if (selectedPacket.getDstPort().contains("80")) {
                    detailsText.append("Note : Le port 80 est souvent utilisé pour le web (HTTP).\n");
                }
                if (selectedPacket.getDstPort().contains("443")) {
                    detailsText.append("Note : Le port 443 est souvent utilisé pour le web sécurisé (HTTPS).\n");
                }
                if (isExpertMode) {
                    // Analyse avancée : Détection de comportements suspects
                    if (selectedPacket.getSrcPort().equals("N/A") || selectedPacket.getDstPort().equals("N/A")) {
                        detailsText.append("Alerte : Ports non définis, possible paquet mal formé.\n");
                    }
                    if (selectedPacket.getSrcIp().equals(selectedPacket.getDstIp())) {
                        detailsText.append("Alerte : IP source et destination identiques, possible boucle locale.\n");
                    }
                }

                Label details = new Label(detailsText.toString());
                details.getStyleClass().add("label");
                details.setWrapText(true);

                detailLayout.getChildren().addAll(detailTitle, details);
                Scene detailScene = new Scene(detailLayout, 400, 300);
                detailScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

                detailStage.setScene(detailScene);
                detailStage.setTitle("Détails du Paquet");
                detailStage.show();
            }
        });

        // Animation d’apparition pour le tableau
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), packetTable);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        analyzeLayout.getChildren().addAll(header, toolbar, infoLabel, packetTable);
        Scene analyzeScene = new Scene(analyzeLayout, 800, 600);
        analyzeScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        analyzeStage.setScene(analyzeScene);
        analyzeStage.setTitle("Analyser Paquets");
        analyzeStage.show();
    }

    private void openExportData() {
        Stage exportStage = new Stage();
        exportStage.initModality(Modality.APPLICATION_MODAL);
        VBox exportLayout = new VBox(20);
        exportLayout.setPadding(new Insets(30));
        exportLayout.setAlignment(Pos.CENTER);
        exportLayout.getStyleClass().add("root");

        Label title = new Label("Exporter les données");
        title.getStyleClass().add("label");

        ComboBox<String> formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll("CSV", "JSON", "TXT");
        formatCombo.getSelectionModel().selectFirst();
        formatCombo.setPrefWidth(300);
        formatCombo.getStyleClass().add("combo-box");

        Button exportButton = createButton("Exporter", "/icons/export.png");
        exportButton.setOnAction(e -> {
            String format = formatCombo.getValue();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le fichier exporté");
            fileChooser.setInitialFileName("captured_packets." + format.toLowerCase());
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                    format + " files (*." + format.toLowerCase() + ")", "*." + format.toLowerCase());
            fileChooser.getExtensionFilters().add(extFilter);
            File file = fileChooser.showSaveDialog(exportStage);
            if (file != null) {
                try {
                    capture.exportData(format, file.getAbsolutePath());
                    statusLabel.setText("Données exportées en " + format + " : " + file.getAbsolutePath());
                } catch (IOException ex) {
                    statusLabel.setText("Erreur lors de l’exportation : " + ex.getMessage());
                }
            } else {
                statusLabel.setText("Exportation annulée.");
            }
            exportStage.close();
        });

        exportLayout.getChildren().addAll(title, formatCombo, exportButton);
        Scene exportScene = new Scene(exportLayout, 400, 250);
        exportScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        exportStage.setScene(exportScene);
        exportStage.setTitle("Exporter Données");
        exportStage.show();
    }

    private void toggleMode() {
        isExpertMode = !isExpertMode;
        statusLabel.setText("Mode " + (isExpertMode ? "Expert" : "Simple") + " activé");
        System.out.println("[INFO] Mode basculé : " + (isExpertMode ? "Expert" : "Simple"));
    }

    private void openShowStats() {
        Stage statsStage = new Stage();
        statsStage.initModality(Modality.APPLICATION_MODAL);
        VBox statsLayout = new VBox(20);
        statsLayout.setPadding(new Insets(30));
        statsLayout.setAlignment(Pos.CENTER);
        statsLayout.getStyleClass().add("root");

        Label title = new Label("Statistiques des Paquets Capturés");
        title.getStyleClass().add("label");

        List<PacketInfo> packets = capture.getCapturedPackets();
        if (packets.isEmpty()) {
            Label noDataLabel = new Label("Aucun paquet capturé pour afficher des statistiques.");
            noDataLabel.getStyleClass().add("label");
            statsLayout.getChildren().addAll(title, noDataLabel);
        } else {
            // Calcul des statistiques
            Map<String, Integer> protocolCounts = new HashMap<>();
            Map<String, Integer> ipCounts = new HashMap<>();
            for (PacketInfo packet : packets) {
                protocolCounts.put(packet.getProtocol(), protocolCounts.getOrDefault(packet.getProtocol(), 0) + 1);
                ipCounts.put(packet.getSrcIp(), ipCounts.getOrDefault(packet.getSrcIp(), 0) + 1);
                ipCounts.put(packet.getDstIp(), ipCounts.getOrDefault(packet.getDstIp(), 0) + 1);
            }

            StringBuilder statsText = new StringBuilder();
            statsText.append("Nombre total de paquets : ").append(packets.size()).append("\n\n");
            statsText.append("Répartition par protocole :\n");
            for (Map.Entry<String, Integer> entry : protocolCounts.entrySet()) {
                statsText.append("- ").append(entry.getKey()).append(" : ").append(entry.getValue())
                        .append(" paquets\n");
            }
            statsText.append("\nActivité par IP :\n");
            for (Map.Entry<String, Integer> entry : ipCounts.entrySet()) {
                statsText.append("- ").append(entry.getKey()).append(" : ").append(entry.getValue())
                        .append(" occurrences\n");
            }

            Label statsLabel = new Label(statsText.toString());
            statsLabel.getStyleClass().add("label");
            statsLabel.setWrapText(true);

            statsLayout.getChildren().addAll(title, statsLabel);
        }

        Scene statsScene = new Scene(statsLayout, 500, 400);
        statsScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        statsStage.setScene(statsScene);
        statsStage.setTitle("Statistiques");
        statsStage.show();
    }

    private void enableStealthMode() {
        isStealthMode = !isStealthMode;
        if (isStealthMode) {
            // En mode furtif, on minimise l’empreinte (par exemple, désactiver les logs
            // inutiles)
            System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s%n");
            statusLabel.setText("Mode furtif activé : logs réduits.");
            System.out.println("[INFO] Mode furtif activé.");
        } else {
            // Restaurer les logs normaux
            System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %2$s %5$s%n");
            statusLabel.setText("Mode furtif désactivé.");
            System.out.println("[INFO] Mode furtif désactivé.");
        }
    }
}