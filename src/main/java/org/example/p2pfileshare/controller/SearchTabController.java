package org.example.p2pfileshare.controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;
import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.model.SearchResult;
import org.example.p2pfileshare.network.control.ControlClient;
import org.example.p2pfileshare.service.FileShareService;
import org.example.p2pfileshare.service.PeerService;
import org.example.p2pfileshare.service.SearchService;

import java.util.List;

public class SearchTabController {

    private SearchService searchService;
    private FileShareService fileShareService;
    private ControlClient controlClient;
    private PeerService peerService;
    private Label globalStatusLabel;

    @FXML private TextField searchField;
    @FXML private TextField filterSubjectField;
    @FXML private TextField filterPeerField;

    @FXML private TableView<SearchResult> searchResultTable;
    @FXML private TableColumn<SearchResult, String> colName;
    @FXML private TableColumn<SearchResult, String> colSubject;
    @FXML private TableColumn<SearchResult, Long> colSize;
    @FXML private TableColumn<SearchResult, String> colOwner;

    @FXML private ProgressBar downloadProgress;
    @FXML private Label downloadStatusLabel;

    @FXML private Button btnDownload;
    @FXML private Button btnPause;
    @FXML private Button btnResume;
    @FXML private Button btnCancel;

    private final ObservableList<SearchResult> searchResults = FXCollections.observableArrayList();

    public void init(SearchService searchService,
                     FileShareService fileShareService,
                     ControlClient controlClient,
                     PeerService peerService,
                     Label globalStatusLabel) {
        this.searchService = searchService;
        this.fileShareService = fileShareService;
        this.controlClient = controlClient;
        this.peerService = peerService;
        this.globalStatusLabel = globalStatusLabel;

        setupTable();
        setupEnterKey();
    }

    private void setupEnterKey() {
        searchField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) onSearch(); });
        filterSubjectField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) onSearch(); });
        filterPeerField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) onSearch(); });
    }

    private void setupTable() {
        // Map các cột với thuộc tính trong SearchResult.java
        colName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));

        // Lưu ý: Trong SearchResult phải có hàm getOwnerName() trả về String
        // Hoặc bạn dùng colOwner.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOwner().getName()));
        colOwner.setCellValueFactory(new PropertyValueFactory<>("ownerName"));

        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colSize.setCellFactory(column -> new TableCell<SearchResult, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatSize(item));
                }
            }
        });

        searchResultTable.setItems(searchResults);
        searchResultTable.setPlaceholder(new Label("Nhập điều kiện và nhấn Tìm kiếm"));
    }

    @FXML
    private void onSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        String subject = filterSubjectField.getText() == null ? "" : filterSubjectField.getText().trim();
        String peerName = filterPeerField.getText() == null ? "" : filterPeerField.getText().trim();

        // check nếu cả 3 điều kiện đều trống
        if (keyword.isEmpty() && subject.isEmpty() && peerName.isEmpty()) {
            showInfoDialog("Cảnh báo", "Thiếu thông tin", "Vui lòng nhập ít nhất một điều kiện tìm kiếm (Tên file, Môn hoặc Peer).", false);
            return;
        }

        // 1. reset trạng thái
        searchResults.clear();
        downloadStatusLabel.setText("Đang gửi yêu cầu tìm kiếm...");

        searchResultTable.setPlaceholder(new Label("⏳ Đang tìm kiếm..."));

        // 2. Lấy danh sách Peer ID đang kết nối từ Client
        List<String> connectedIds = controlClient.getPeerIdList();
        if (connectedIds == null || connectedIds.isEmpty()) {
            downloadStatusLabel.setText("Không có peer nào kết nối.");
            searchResultTable.setPlaceholder(new Label("Không có kết nối nào."));
            return;
        }

        // 3. Chuyển đổi ID sang PeerInfo để lấy IP/Port gửi lệnh
        List<PeerInfo> connectedPeers = peerService.getPeersByIds(connectedIds);

        int sentCount = 0;
        for (PeerInfo p : connectedPeers) {
            // Bỏ qua chính mình
            if (p.getPeerId().equals(peerService.getMyPeerId())) continue;

            // Gửi lệnh SEARCH_REQ
            controlClient.sendSearchRequest(p, keyword);
            sentCount++;
        }

        downloadStatusLabel.setText("Đã gửi yêu cầu đến " + sentCount + " peer.");

        // Sau 3 giây nếu vẫn chưa có kết quả thì hiện thông báo không tìm thấy
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> {
            if (searchResults.isEmpty()) {
                searchResultTable.setPlaceholder(new Label("❌ Không tìm thấy kết quả nào phù hợp."));
                downloadStatusLabel.setText("Hoàn tất tìm kiếm. Không có kết quả.");
            }
        });
        delay.play();
    }


    public void onReceiveSearchResult(PeerInfo sender, String fileData) {
        Platform.runLater(() -> {
            try {
                String[] parts = fileData.split(":");
                if (parts.length >= 2) {
                    String fName = parts[0];
                    long fSize = 0;
                    try { fSize = Long.parseLong(parts[1]); } catch (Exception e) {}
                    String fSubject = parts.length > 2 ? parts[2] : "Khác";

                    // Lấy lại giá trị filter hiện tại
                    String curKey = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
                    String curSub = filterSubjectField.getText() == null ? "" : filterSubjectField.getText().trim().toLowerCase();
                    String curPeer = filterPeerField.getText() == null ? "" : filterPeerField.getText().trim().toLowerCase();

                    // Lọc tên file (nếu server trả về hết do dùng "*")
                    if (!curKey.isEmpty() && !fName.toLowerCase().contains(curKey)) return;

                    // Lọc môn học
                    if (!curSub.isEmpty() && !fSubject.toLowerCase().contains(curSub)) return;

                    // Lọc tên Peer
                    if (!curPeer.isEmpty() && !sender.getName().toLowerCase().contains(curPeer)) return;

                    // Thêm vào bảng
                    SearchResult result = new SearchResult(fName, fSize, fSubject, sender);
                    searchResults.add(result);
                    downloadStatusLabel.setText("Tìm thấy " + searchResults.size() + " kết quả.");
                }
            } catch (Exception e) {
                System.err.println("Lỗi parse: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onViewFileDetails() {
        SearchResult selected = searchResultTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String content = "Kích thước: " + formatSize(selected.getSize()) + "\n" +
                "Môn học: " + selected.getSubject() + "\n" +
                "Chủ sở hữu: " + selected.getOwner().getName() + "\n" +
                "IP: " + selected.getOwner().getIp();

        showInfoDialog("Chi tiết file", selected.getFileName(), content, true);
    }

    @FXML
    public void onDownloadSelected(ActionEvent actionEvent) {
        downloadProgress.setProgress(0.3);
        downloadStatusLabel.setText("Đang tải (demo)...");

        if (btnDownload != null) btnDownload.setDisable(true); // Đang tải thì khóa nút tải
        if (btnPause != null) btnPause.setDisable(false);      // Mở nút Pause
        if (btnResume != null) btnResume.setDisable(true);     // Khóa nút Resume
        if (btnCancel != null) btnCancel.setDisable(false);    // Mở nút Cancel
    }

    @FXML
    public void onPauseDownload(ActionEvent actionEvent) {

    }

    @FXML
    public void onResumeDownload(ActionEvent actionEvent) {

    }

    @FXML
    public void onCancelDownload(ActionEvent actionEvent) {

    }

    // Hàm reset trạng thái nút về ban đầu
    private void resetButtons() {
        if (btnDownload != null) btnDownload.setDisable(false);
        if (btnPause != null) btnPause.setDisable(true);
        if (btnResume != null) btnResume.setDisable(true);
        if (btnCancel != null) btnCancel.setDisable(true);
    }


    private String formatSize(long bytes) {
        if (bytes >= 1024 * 1024 * 1024) return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        if (bytes >= 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        if (bytes >= 1024) return String.format("%.2f KB", bytes / 1024.0);
        return bytes + " B";
    }

    // custom dialog
    private void showInfoDialog(String title, String header, String content, boolean isSuccess) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/org/example/p2pfileshare/ConfirmationDialog.fxml"));
            javafx.scene.Parent page = loader.load();

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            if (searchResultTable.getScene() != null) {
                dialogStage.initOwner(searchResultTable.getScene().getWindow());
            }

            javafx.scene.Scene scene = new javafx.scene.Scene(page);
            dialogStage.setScene(scene);

            ConfirmationController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setContent(title, header, content, "Đóng");

            if (isSuccess) controller.setStyleSuccess();
            else controller.setStyleDanger();

            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
