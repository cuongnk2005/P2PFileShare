package org.example.p2pfileshare.service;

public class HistoryService {

    // TODO: sau này lưu lịch sử vào file JSON/SQLite

    public void addRecord(String fileName, String peer, long sizeBytes) {
        // demo: chưa lưu thật
        System.out.println("[History] Tải file " + fileName + " từ " + peer + " (" + sizeBytes + " bytes)");
    }

    public void clear() {
        // demo
        System.out.println("[History] Clear all (demo)");
    }
}
