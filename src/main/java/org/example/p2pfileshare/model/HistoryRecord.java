package org.example.p2pfileshare.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HistoryRecord {

    private String fileName;
    private String sourcePeer;
    private long size;
    private String date; // format yyyy-MM-dd HH:mm

    public HistoryRecord(String fileName, String sourcePeer, long size) {
        this.fileName = fileName;
        this.sourcePeer = sourcePeer;
        this.size = size;
        this.date = currentDate();
    }

    // ===== GETTER =====
    public String getFileName() { return fileName; }
    public String getSourcePeer() { return sourcePeer; }
    public long getSize() { return size; }
    public String getDate() { return date; }

    // ===== JSON SUPPORT =====
    private static final Gson gson = new Gson();

    public static List<HistoryRecord> fromJsonArray(String json) {
        return gson.fromJson(json, new TypeToken<List<HistoryRecord>>(){}.getType());
    }

    public static String toJsonArray(List<HistoryRecord> list) {
        return gson.toJson(list);
    }

    private String currentDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
    }
}
