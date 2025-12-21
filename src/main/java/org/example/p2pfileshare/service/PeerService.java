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

    // quét tìm Peer
    public List<PeerInfo> scanPeers() {
        List<PeerInfo> found = PeerDiscovery.discoverPeers(myPeerId, 3000);
        discoveredPeers.clear();
        discoveredPeers.addAll(found);
        return found;
    }

    // tìm peer theo name
    public PeerInfo getPeerFromName(String name) {
        for (PeerInfo p : discoveredPeers) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    // Lấy danh sách Peer, nếu chưa quét thì quét trước
    public List<PeerInfo> getListPeer() {
        if (discoveredPeers.isEmpty()) scanPeers();
        return discoveredPeers;
    }

    // tìm peer theo id
    public PeerInfo getPeerFromId(String peerId) {
        for (PeerInfo p : discoveredPeers) {
            if (p.getPeerId().equals(peerId)) {
                return p;
            }
        }
        return null;
    }


    // lấy danh sách peer
    public List<PeerInfo> getDiscoveredPeers() {
        return discoveredPeers;
    }

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
