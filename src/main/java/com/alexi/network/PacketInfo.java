package com.alexi.network;

public class PacketInfo {
    private String timestamp;
    private String srcIp;
    private String dstIp;
    private String srcPort;
    private String dstPort;
    private String protocol;

    public PacketInfo(String timestamp, String srcIp, String dstIp, String srcPort, String dstPort, String protocol) {
        this.timestamp = timestamp;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.protocol = protocol;
    }

    // Getters nécessaires pour PropertyValueFactory
    public String getTimestamp() {
        return timestamp;
    }

    public String getSrcIp() {
        return srcIp;
    }

    public String getDstIp() {
        return dstIp;
    }

    public String getSrcPort() {
        return srcPort;
    }

    public String getDstPort() {
        return dstPort;
    }

    public String getProtocol() {
        return protocol;
    }

    // Pour le débogage
    @Override
    public String toString() {
        return "PacketInfo{" +
                "timestamp='" + timestamp + '\'' +
                ", srcIp='" + srcIp + '\'' +
                ", dstIp='" + dstIp + '\'' +
                ", srcPort='" + srcPort + '\'' +
                ", dstPort='" + dstPort + '\'' +
                ", protocol='" + protocol + '\'' +
                '}';
    }
}