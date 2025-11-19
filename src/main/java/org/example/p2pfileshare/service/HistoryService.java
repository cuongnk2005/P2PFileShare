package org.example.p2pfileshare.service;

import org.example.p2pfileshare.model.HistoryRecord;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class HistoryService {

    private final File historyFile = new File("history.json");

    /** Lưu 1 record */
    public void add(HistoryRecord record) {
        List<HistoryRecord> list = loadHistory();
        list.add(record);
        saveHistory(list);
    }

    /** Trả về toàn bộ lịch sử */
    public List<HistoryRecord> loadHistory() {
        try {
            if (!historyFile.exists()) return new ArrayList<>();
            String json = Files.readString(historyFile.toPath());
            return HistoryRecord.fromJsonArray(json);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /** Xóa lịch sử */
    public void clear() {
        if (historyFile.exists()) historyFile.delete();
    }

    private void saveHistory(List<HistoryRecord> list) {
        try (FileWriter fw = new FileWriter(historyFile)) {
            fw.write(HistoryRecord.toJsonArray(list));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Controller gọi để refresh UI
    public void refresh() {
        System.out.println("[HistoryService] refresh");
    }
}
