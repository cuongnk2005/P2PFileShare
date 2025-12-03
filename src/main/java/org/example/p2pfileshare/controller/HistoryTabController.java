package org.example.p2pfileshare.controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.p2pfileshare.model.DownloadHistory;
import org.example.p2pfileshare.service.HistoryService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistoryTabController {

    private HistoryService historyService;
    private Label globalStatusLabel;

    @FXML private TableView<DownloadHistory> historyTable;

    private final ObservableList<DownloadHistory> histories = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // keep reference to remove later if needed
    private Runnable historyChangeListener;

    public void init(HistoryService historyService, Label globalStatusLabel) {
        this.historyService = historyService;
        this.globalStatusLabel = globalStatusLabel;

        // configure table
        if (historyTable == null) return;

        historyTable.setItems(histories);
        historyTable.getColumns().clear();

        TableColumn<DownloadHistory, String> colName = new TableColumn<>("File");
        colName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        colName.setPrefWidth(180);

        TableColumn<DownloadHistory, String> colPath = new TableColumn<>("Saved Path");
        colPath.setCellValueFactory(new PropertyValueFactory<>("savedPath"));
        colPath.setPrefWidth(360);

        TableColumn<DownloadHistory, String> colPeer = new TableColumn<>("Peer");
        colPeer.setCellValueFactory(cell -> {
            DownloadHistory d = cell.getValue();
            String v = (d.getPeerName() == null ? "" : d.getPeerName()) +
                    (d.getPeerIp() == null || d.getPeerIp().isBlank() ? "" : " (" + d.getPeerIp() + ")");
            return new ReadOnlyStringWrapper(v);

        });
        colPeer.setPrefWidth(160);

        TableColumn<DownloadHistory, String> colDate = new TableColumn<>("Downloaded At");
        colDate.setCellValueFactory(cell -> {
            DownloadHistory d = cell.getValue();
            String s = d.getDownloadDate() != null ? d.getDownloadDate().format(dateFormatter) : "";
            return new ReadOnlyStringWrapper(s);
        });
        colDate.setPrefWidth(140);

        historyTable.getColumns().addAll(colName, colPath, colPeer, colDate);

        // initial load
        refreshHistory();

        // register listener so UI auto refreshes when history file changes
        if (this.historyService != null) {
            historyChangeListener = () -> Platform.runLater(this::refreshHistory);
            this.historyService.addHistoryChangeListener(historyChangeListener);
            System.out.println("da dang ky callback thanh cong");
        }
    }

    @FXML
    private void onRefreshHistory() {
        refreshHistory();
        if (globalStatusLabel != null) {
            globalStatusLabel.setText("Lịch sử đã nạp: " + histories.size() + " mục");
        }
    }

    private void refreshHistory() {
        if (historyService == null) return;
        List<DownloadHistory> list = historyService.listHistories();
        histories.setAll(list);
        System.out.println("da goi lai ham callback thanh cong - so muc: " + list.size());
    }

    @FXML
    private void onClearHistory() {
        if (historyService == null) {
            new Alert(Alert.AlertType.WARNING, "History service chưa sẵn sàng.").showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có muốn xóa toàn bộ lịch sử tải không?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                historyService.clearHistory();
                refreshHistory();
                if (globalStatusLabel != null) {
                    globalStatusLabel.setText("Lịch sử đã xóa");
                }
            }
        });
    }

    // optional: call this when controller is disposed to avoid leaks
    public void dispose() {
        if (historyService != null && historyChangeListener != null) {
            historyService.removeHistoryChangeListener(historyChangeListener);
        }
    }
}
