package org.example.p2pfileshare.service;

import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.network.discovery.PeerDiscovery;

import java.util.ArrayList;
import java.util.List;

public class PeerService {

    private final String myPeerId;
    private String myDisplayName; // cho phép thay đổi runtime
    private final int fileServerPort;
    private final int controlPort;

    // Danh sách peer tìm thấy trong LAN
    private final List<PeerInfo> discoveredPeers = new ArrayList<>();

    public PeerService(String myPeerId, String myDisplayName, int fileServerPort, int controlPort) {
        this.myPeerId = myPeerId;
        this.myDisplayName = myDisplayName;
        this.fileServerPort = fileServerPort;
        this.controlPort = controlPort;
    }

    /**
     * Quét peer trong LAN (broadcast).
     */
    public List<PeerInfo> scanPeers() {
        List<PeerInfo> found = PeerDiscovery.discoverPeers(myPeerId, 3000);
        discoveredPeers.clear();
        discoveredPeers.addAll(found);
        return found;
    }

    /**
     * Tìm PeerInfo từ tên (displayName). Nếu có nhiều peer cùng tên, trả peer đầu tiên.
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
     * Tìm PeerInfo từ peerId (UUID) — khuyến nghị sử dụng.
     */
    public PeerInfo getPeerFromId(String peerId) {
        for (PeerInfo p : discoveredPeers) {
            if (p.getPeerId().equals(peerId)) {
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

    // ================= GETTER/SETTER =================

    public String getMyPeerId() {
        return myPeerId;
    }

    public String getMyDisplayName() {
        return myDisplayName;
    }

    public void setMyDisplayName(String myDisplayName) {
        this.myDisplayName = myDisplayName;
    }

    public int getFileServerPort() {
        return fileServerPort;
    }

    public int getControlPort() {
        return controlPort;
    }
}
