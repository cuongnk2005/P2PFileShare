package org.example.p2pfileshare.service;

import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.network.discovery.PeerDiscovery;

import java.util.ArrayList;
import java.util.List;

public class PeerService {

    private final String myName;
    private final int fileServerPort;
    private final int controlPort;

    // Danh sách peer tìm thấy trong LAN
    private final List<PeerInfo> discoveredPeers = new ArrayList<>();

    public PeerService(String myName, int fileServerPort, int controlPort) {
        this.myName = myName;
        this.fileServerPort = fileServerPort;
        this.controlPort = controlPort;
    }

    /**
     * Quét peer trong LAN (broadcast).
     */
    public List<PeerInfo> scanPeers() {
        List<PeerInfo> found = PeerDiscovery.discoverPeers(myName, 3000);
        discoveredPeers.clear();
        discoveredPeers.addAll(found);
        return found;
    }

    /**
     * Tìm PeerInfo từ tên.
     */
    public PeerInfo getPeerFromName(String name) {
        for (PeerInfo p : discoveredPeers) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Lấy danh sách tất cả peers đã quét.
     */
    public List<PeerInfo> getDiscoveredPeers() {
        return discoveredPeers;
    }

    // ================= GETTER =================

    public String getMyName() {
        return myName;
    }

    public int getFileServerPort() {
        return fileServerPort;
    }

    public int getControlPort() {
        return controlPort;
    }
}
