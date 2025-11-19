package org.example.p2pfileshare.model;

public class SearchResult {

    private String fileName;
    private String subject;
    private String ownerName;
    private String peerIp;
    private int peerPort;
    private long size;

    public SearchResult(String fileName, String subject, String ownerName,
                        String peerIp, int peerPort, long size) {
        this.fileName = fileName;
        this.subject = subject;
        this.ownerName = ownerName;
        this.peerIp = peerIp;
        this.peerPort = peerPort;
        this.size = size;
    }

    // ===== GETTERS =====
    public String getFileName() { return fileName; }
    public String getSubject() { return subject; }
    public String getOwnerName() { return ownerName; }
    public String getPeerIp() { return peerIp; }
    public int getPeerPort() { return peerPort; }
    public long getSize() { return size; }
}
