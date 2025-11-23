package org.example.p2pfileshare.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.p2pfileshare.service.HistoryService;

public class HistoryTabController {

    private HistoryService historyService;
    private Label globalStatusLabel;

    @FXML private TableView<?> historyTable;

    private final ObservableList<Object> histories = FXCollections.observableArrayList();

    public void init(HistoryService historyService, Label globalStatusLabel) {
        this.historyService = historyService;
        this.globalStatusLabel = globalStatusLabel;

        // TODO: nếu có model HistoryRecord, gắn TableView<HistoryRecord> và columns
    }

    @FXML
    private void onRefreshHistory() {
        // TODO: load từ HistoryService
        Alert a = new Alert(Alert.AlertType.INFORMATION, "Demo: refresh lịch sử tải.");
        a.showAndWait();
    }

    @FXML
    private void onClearHistory() {
        // TODO: gọi historyService.clear()
        Alert a = new Alert(Alert.AlertType.INFORMATION, "Demo: xóa lịch sử (chưa xóa thật).");
        a.showAndWait();
    }
}
