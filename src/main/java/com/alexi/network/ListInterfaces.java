package com.alexi.network;

import org.pcap4j.core.Pcaps;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import java.util.List;

/**
 * Classe pour lister les interfaces réseau disponibles.
 */

public class ListInterfaces {
    public static void main(String[] args) throws PcapNativeException {

        List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();  // Récupère toutes les interfaces réseau disponibles
        for (PcapNetworkInterface dev : allDevs) {
            System.out.println(dev.getName() + " : " + dev.getDescription());
        }
    }
}