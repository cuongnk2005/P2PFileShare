package org.example.p2pfileshare.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.p2pfileshare.network.control.ControlClient;
import org.example.p2pfileshare.service.FileShareService;
import org.example.p2pfileshare.service.SearchService;

public class SearchTabController {

    private SearchService searchService;
    private FileShareService fileShareService;
    private ControlClient controlClient;
    private Label globalStatusLabel;

    @FXML private TextField searchField;
    @FXML private TextField filterSubjectField;
    @FXML private TextField filterPeerField;

    @FXML private TableView<?> searchResultTable;
    @FXML private ProgressBar downloadProgress;
    @FXML private Label downloadStatusLabel;

    private final ObservableList<Object> searchResults = FXCollections.observableArrayList();

    public void init(SearchService searchService,
                     FileShareService fileShareService,
                     ControlClient controlClient,
                     Label globalStatusLabel) {
        this.searchService = searchService;
        this.fileShareService = fileShareService;
        this.controlClient = controlClient;
        this.globalStatusLabel = globalStatusLabel;

        // TODO: nếu có model SearchResult riêng, gắn TableView<T> tương ứng
    }

    @FXML
    private void onSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        String subject = filterSubjectField.getText() == null ? "" : filterSubjectField.getText().trim();
        String peer    = filterPeerField.getText() == null ? "" : filterPeerField.getText().trim();

        // TODO: hiện tại demo, chưa implement thực sự
        downloadStatusLabel.setText("Đã gửi yêu cầu tìm: " + keyword + " (demo)");
    }

    @FXML
    private void onViewFileDetails() {
        Alert a = new Alert(Alert.AlertType.INFORMATION,
                "Demo: Hiển thị thông tin chi tiết file (sau này lấy từ SearchResult).");
        a.showAndWait();
    }

    @FXML
    private void onDownloadFile() {
        // TODO: sau này, lấy thông tin file & peer từ searchResultTable rồi gọi fileShareService.download(...)
        downloadProgress.setProgress(0.3);
        downloadStatusLabel.setText("Đang tải (demo)...");
    }
}
