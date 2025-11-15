package org.example.p2pfileshare.Model;

public class PeerInfo {
    private final String name;
    private final String ip;
    private final int port;
    private final String status;

    public PeerInfo(String name, String ip, int port, String status) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.status = status;
    }

    public String getName()   { return name; }
    public String getIp()     { return ip; }
    public int getPort()      { return port; }
    public String getStatus() { return status; }
}