package com.alexi.network;

import org.pcap4j.packet.IpV4Packet; // Pour les paquets IPv4
import org.pcap4j.packet.Packet; // Classe générique pour tous les paquets
import org.pcap4j.packet.TcpPacket; // Pour les paquets TCP
import org.pcap4j.packet.UdpPacket; // Pour les paquets UDP
import java.time.LocalDateTime; // Pour ajouter un timestamp précis

/**
 * Classe qui analyse les trames réseau capturées et retourne les informations
 * clés pour une interface graphique.
 */
public class PacketAnalyzer {

    /**
     * Analyse un paquet réseau et retourne un objet PacketInfo pour la GUI.
     * 
     * @param packet
     * @return PacketInfo contenant les détails extraits du paquet
     */
    public static PacketInfo analyzeForUI(Packet packet) {
        // Valeurs par défaut si certaines informations ne sont pas trouvées
        String timestamp = LocalDateTime.now().toString(); // Timestamp actuel
        String srcIp = "N/A"; // IP source
        String dstIp = "N/A"; // IP destination
        String srcPort = "N/A"; // Port source
        String dstPort = "N/A"; // Port destination
        String protocol = "Inconnu"; // Protocole

        try {
            // Vérifie si le paquet contient un en-tête IPv4
            if (packet.contains(IpV4Packet.class)) {
                IpV4Packet ipPacket = packet.get(IpV4Packet.class);
                srcIp = ipPacket.getHeader().getSrcAddr().toString();
                dstIp = ipPacket.getHeader().getDstAddr().toString();
            }

            // Si c’est un paquet TCP
            if (packet.contains(TcpPacket.class)) {
                TcpPacket tcpPacket = packet.get(TcpPacket.class);
                TcpPacket.TcpHeader tcpHeader = tcpPacket.getHeader();
                srcPort = tcpHeader.getSrcPort().toString();
                dstPort = tcpHeader.getDstPort().toString();
                protocol = "TCP";
            }
            // Si c’est un paquet UDP
            else if (packet.contains(UdpPacket.class)) {
                UdpPacket udpPacket = packet.get(UdpPacket.class);
                srcPort = udpPacket.getHeader().getSrcPort().toString();
                dstPort = udpPacket.getHeader().getDstPort().toString();
                protocol = "UDP";
            }

        } catch (Exception e) {
            System.out.println("[ERROR] Erreur lors de l’analyse du paquet : " + e.getMessage());
        }

        // Retourne un objet PacketInfo avec les informations extraites
        return new PacketInfo(timestamp, srcIp, dstIp, srcPort, dstPort, protocol);
    }
}