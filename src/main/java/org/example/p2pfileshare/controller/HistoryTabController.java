package org.example.p2pfileshare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.p2pfileshare.service.HistoryService;

public class HistoryTabController {

    private HistoryService historyService;
    private Label globalStatusLabel;

    @FXML private TableView<?> historyTable;

    public void init(HistoryService historyService, Label globalStatusLabel) {
        this.historyService = historyService;
        this.globalStatusLabel = globalStatusLabel;
    }

    @FXML
    private void onRefreshHistory() {
        historyService.refresh();
        globalStatusLabel.setText("Đã tải lại lịch sử (demo)");
    }

    @FXML
    private void onClearHistory() {
        globalStatusLabel.setText("Đã xóa lịch sử (demo)");
    }
}
