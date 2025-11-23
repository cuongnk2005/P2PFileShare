package org.example.p2pfileshare.model;

public class PeerInfo {

    public enum ConnectionState {
        NOT_CONNECTED,
        PENDING,
        CONNECTED,
        REJECTED
    }

    private final String name;
    private final String ip;
    private final int fileServerPort;   // port dùng để tải file
    private final int controlPort;      // port dùng cho control (connect, list...)
    private String status;              // "Online", "Offline" (từ Discovery)
    private ConnectionState connectionState;

    public PeerInfo(String name,
                    String ip,
                    int fileServerPort,
                    int controlPort,
                    String status) {
        this.name = name;
        this.ip = ip;
        this.fileServerPort = fileServerPort;
        this.controlPort = controlPort;
        this.status = status;
        this.connectionState = ConnectionState.NOT_CONNECTED;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public int getFileServerPort() {
        return fileServerPort;
    }

    public int getControlPort() {
        return controlPort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(ConnectionState connectionState) {
        this.connectionState = connectionState;
    }
}