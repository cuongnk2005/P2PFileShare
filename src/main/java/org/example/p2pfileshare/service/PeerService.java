package org.example.p2pfileshare.service;

import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.network.discovery.PeerDiscovery;

import java.util.List;

public class PeerService {

    private final String myName;
    private final int fileServerPort;

    public PeerService(String myName, int fileServerPort) {
        this.myName = myName;
        this.fileServerPort = fileServerPort;

        // Khởi động responder UDP để peer khác tìm thấy mình
        PeerDiscovery.startResponder(myName, fileServerPort);
    }

    /** Quét peer trong LAN */
    public List<PeerInfo> scanPeers() {
        return PeerDiscovery.discoverPeers(myName, 3000);
    }
}
