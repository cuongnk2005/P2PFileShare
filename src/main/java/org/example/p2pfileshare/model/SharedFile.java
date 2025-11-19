package org.example.p2pfileshare.model;

public class SharedFile {

    private String name;
    private String type;
    private long size;
    private String subject;
    private String tags;
    private boolean visible;

    public SharedFile(String name, String type, long size,
                      String subject, String tags, boolean visible) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.subject = subject;
        this.tags = tags;
        this.visible = visible;
    }

    // ===== GETTER =====
    public String getName() { return name; }
    public String getType() { return type; }
    public long getSize() { return size; }
    public String getSubject() { return subject; }
    public String getTags() { return tags; }
    public boolean isVisible() { return visible; }

    // ===== SETTER =====
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
