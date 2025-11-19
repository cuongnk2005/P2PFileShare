package org.example.p2pfileshare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.p2pfileshare.service.FileShareService;
import org.example.p2pfileshare.service.SearchService;

public class SearchTabController {

    private SearchService searchService;
    private FileShareService fileShareService;
    private Label globalStatusLabel;

    @FXML private TextField searchField;
    @FXML private TextField filterSubjectField;
    @FXML private TextField filterPeerField;

    @FXML private TableView<?> searchResultTable;

    @FXML private ProgressBar downloadProgress;
    @FXML private Label downloadStatusLabel;

    public void init(SearchService searchService,
                     FileShareService fileShareService,
                     Label globalStatusLabel) {

        this.searchService = searchService;
        this.fileShareService = fileShareService;
        this.globalStatusLabel = globalStatusLabel;
    }

    @FXML
    private void onSearch() {
        String keyword = searchField.getText();
        searchService.search(keyword);
        downloadStatusLabel.setText("Đang tìm: " + keyword + " (demo)");
    }

    @FXML
    private void onViewFileDetails() {
        new Alert(Alert.AlertType.INFORMATION,
                "Demo: xem chi tiết file").showAndWait();
    }

    @FXML
    private void onDownloadFile() {
        downloadProgress.setProgress(0.3);
        downloadStatusLabel.setText("Đang tải (demo)...");
    }
}
