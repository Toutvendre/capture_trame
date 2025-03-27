package com.alexi.network;

import org.pcap4j.core.*;
import javafx.application.Platform;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PacketCapture {

    private volatile boolean isCapturing = false;
    private Thread captureThread;
    private PacketCaptureUI ui;
    private PcapHandle handle;
    private PcapNetworkInterface selectedInterface;
    private List<PacketInfo> capturedPackets = new ArrayList<>();
    private String currentFilter = "tcp or udp";

    public PacketCapture(PacketCaptureUI ui) {
        this.ui = ui;
    }

    public void setUI(PacketCaptureUI ui) {
        this.ui = ui;
    }

    public void detectInterfaces() {
        try {
            List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
            if (allDevs.isEmpty()) {
                System.out.println("[ERROR] Aucune interface détectée.");
                if (ui != null)
                    Platform.runLater(() -> ui.showError("Aucune interface détectée."));
                return;
            }
            System.out.println("[INFO] Interfaces disponibles :");
            for (int i = 0; i < allDevs.size(); i++) {
                PcapNetworkInterface dev = allDevs.get(i);
                System.out.println(i + ": " + dev.getName() + " (" + dev.getDescription() + ") - Actif : " + dev.isUp()
                        + " - Adresses : " + dev.getAddresses());
            }
        } catch (PcapNativeException e) {
            System.out.println("[ERROR] Erreur détection : " + e.getMessage());
            if (ui != null)
                Platform.runLater(() -> ui.showError("Erreur détection : " + e.getMessage()));
        }
    }

    public void startCapture() throws PcapNativeException {
        if (isCapturing) {
            System.out.println("[INFO] Capture déjà en cours.");
            return;
        }
        isCapturing = true;
        capturedPackets.clear();

        captureThread = new Thread(() -> {
            try {
                List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
                if (allDevs.isEmpty()) {
                    System.out.println("[ERROR] Aucune interface détectée.");
                    if (ui != null)
                        Platform.runLater(() -> ui.showError("Aucune interface détectée."));
                    return;
                }

                PcapNetworkInterface nif = selectedInterface;
                if (nif == null) {
                    System.out.println("[WARNING] Aucune interface sélectionnée, choix automatique...");
                    for (PcapNetworkInterface dev : allDevs) {
                        String desc = dev.getDescription() != null ? dev.getDescription().toLowerCase() : "";
                        if (dev.isUp() && !dev.getAddresses().isEmpty() &&
                                !desc.contains("loopback") && !desc.contains("bluetooth") &&
                                !desc.contains("wan miniport") && !desc.contains("virtual")) {
                            nif = dev;
                            break;
                        }
                    }
                    if (nif == null)
                        nif = allDevs.get(0);
                }
                System.out.println("[INFO] Interface utilisée : " + nif.getName() + " (" + nif.getDescription() + ")");

                int snapshotLength = 65536;
                int timeout = 10;
                handle = nif.openLive(snapshotLength, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, timeout);
                if (handle == null) {
                    System.out.println("[ERROR] Impossible d’ouvrir l’interface.");
                    if (ui != null)
                        Platform.runLater(() -> ui.showError("Impossible d’ouvrir l’interface."));
                    return;
                }

                String filterToApply = currentFilter.isEmpty() ? "tcp or udp" : currentFilter;
                System.out.println("[INFO] Application du filtre : " + filterToApply);
                handle.setFilter(filterToApply, BpfProgram.BpfCompileMode.OPTIMIZE);

                PacketListener listener = packet -> {
                    try {
                        PacketInfo info = PacketAnalyzer.analyzeForUI(packet);
                        if (info != null) {
                            capturedPackets.add(info);
                            System.out.println("[INFO] Paquet capturé et stocké : " + info);
                            if (ui != null) {
                                Platform.runLater(() -> ui.addPacketToTable(info));
                            } else {
                                System.out.println("[WARNING] UI est null, impossible d’ajouter le paquet à la table.");
                            }
                        } else {
                            System.out.println("[WARNING] Paquet non analysé : " + packet);
                        }
                    } catch (Exception e) {
                        System.out.println("[ERROR] Erreur lors de l’analyse du paquet : " + e.getMessage());
                        e.printStackTrace();
                    }
                };

                System.out.println("[INFO] Début de la capture...");
                handle.loop(-1, listener);
            } catch (PcapNativeException e) {
                System.out.println("[ERROR] Erreur Pcap : " + e.getMessage());
                if (ui != null)
                    Platform.runLater(() -> ui.showError("Erreur Pcap : " + e.getMessage()));
            } catch (InterruptedException e) {
                System.out.println("[INFO] Capture interrompue par l’utilisateur.");
            } catch (Exception e) {
                System.out.println("[ERROR] Erreur inattendue dans la capture : " + e.getMessage());
                e.printStackTrace();
                if (ui != null)
                    Platform.runLater(() -> ui.showError("Erreur inattendue : " + e.getMessage()));
            } finally {
                if (handle != null && handle.isOpen()) {
                    handle.close();
                    System.out.println("[INFO] Handle fermé.");
                }
                isCapturing = false;
                System.out.println("[INFO] Capture terminée. Paquets stockés : " + capturedPackets.size());
                if (ui != null)
                    Platform.runLater(() -> ui.captureStopped());
            }
        });
        captureThread.setDaemon(true);
        captureThread.start();
    }

    public void stopCapture() {
        if (!isCapturing) {
            System.out.println("[INFO] Aucune capture en cours.");
            return;
        }
        isCapturing = false;
        if (handle != null && handle.isOpen()) {
            try {
                handle.breakLoop();
            } catch (NotOpenException e) {
                System.out.println("[ERROR] Erreur arrêt : " + e.getMessage());
            }
        }
        if (captureThread != null)
            captureThread.interrupt();
    }

    public void setSelectedInterface(PcapNetworkInterface nif) {
        this.selectedInterface = nif;
        System.out.println("[INFO] Interface sélectionnée : " + (nif != null ? nif.getName() : "Aucune"));
    }

    public void setFilter(String filter) {
        this.currentFilter = filter;
        System.out.println("[INFO] Filtre défini : " + this.currentFilter);
    }

    public List<PacketInfo> getCapturedPackets() {
        return capturedPackets;
    }

    public void exportData(String format, String filePath) throws IOException {
        if (capturedPackets.isEmpty()) {
            System.out.println("[WARNING] Aucun paquet à exporter.");
            return;
        }

        switch (format.toLowerCase()) {
            case "txt":
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                    for (PacketInfo packet : capturedPackets) {
                        writer.write(packet.toString());
                        writer.newLine();
                    }
                    System.out.println("[INFO] Données exportées en TXT : " + filePath);
                }
                break;
            case "csv":
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                    // En-tête CSV
                    writer.write("Timestamp,IP Source,IP Destination,Port Source,Port Destination,Protocole");
                    writer.newLine();
                    for (PacketInfo packet : capturedPackets) {
                        writer.write(String.format("%s,%s,%s,%s,%s,%s",
                                packet.getTimestamp(),
                                packet.getSrcIp(),
                                packet.getDstIp(),
                                packet.getSrcPort(),
                                packet.getDstPort(),
                                packet.getProtocol()));
                        writer.newLine();
                    }
                    System.out.println("[INFO] Données exportées en CSV : " + filePath);
                }
                break;
            case "json":
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                    writer.write("[\n");
                    for (int i = 0; i < capturedPackets.size(); i++) {
                        PacketInfo packet = capturedPackets.get(i);
                        writer.write(String.format(
                                "  {\"timestamp\": \"%s\", \"srcIp\": \"%s\", \"dstIp\": \"%s\", \"srcPort\": \"%s\", \"dstPort\": \"%s\", \"protocol\": \"%s\"}",
                                packet.getTimestamp(),
                                packet.getSrcIp(),
                                packet.getDstIp(),
                                packet.getSrcPort(),
                                packet.getDstPort(),
                                packet.getProtocol()));
                        if (i < capturedPackets.size() - 1) {
                            writer.write(",");
                        }
                        writer.newLine();
                    }
                    writer.write("]");
                    System.out.println("[INFO] Données exportées en JSON : " + filePath);
                }
                break;
            default:
                System.out.println("[ERROR] Format non supporté : " + format);
        }
    }
}