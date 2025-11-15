package org.example.p2pfileshare;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class MainController {

    // ===== MODEL CHO PEER =====
    public static class PeerInfo {
        private final String name;
        private final String ip;
        private final int port;
        private final String status;

        public PeerInfo(String name, String ip, int port, String status) {
            this.name = name;
            this.ip = ip;
            this.port = port;
            this.status = status;
        }

        public String getName()   { return name; }
        public String getIp()     { return ip; }
        public int getPort()      { return port; }
        public String getStatus() { return status; }
    }

    // ===== DANH SÁCH PEER (DÙNG CHO TABLEVIEW) =====
    private final ObservableList<PeerInfo> peerList = FXCollections.observableArrayList();

    // PEER TAB
    @FXML private TableView<PeerInfo> peerTable;
    @FXML private TableColumn<PeerInfo, String> colPeerName;
    @FXML private TableColumn<PeerInfo, String> colPeerIp;
    @FXML private TableColumn<PeerInfo, Number> colPeerPort;
    @FXML private TableColumn<PeerInfo, String> colPeerStatus;
    @FXML private Label peerStatusLabel;

    // SHARE TAB
    @FXML private TextField shareFolderField;
    @FXML private TableView<?> sharedFileTable;
    @FXML private TableColumn<?, ?> colSharedName;
    @FXML private TableColumn<?, ?> colSharedType;
    @FXML private TableColumn<?, ?> colSharedSize;
    @FXML private TableColumn<?, ?> colSharedSubject;
    @FXML private TableColumn<?, ?> colSharedTags;
    @FXML private TableColumn<?, ?> colSharedVisibility;

    // SEARCH TAB
    @FXML private TextField searchField;
    @FXML private TextField filterSubjectField;
    @FXML private TextField filterPeerField;
    @FXML private TableView<?> searchResultTable;
    @FXML private TableColumn<?, ?> colResultName;
    @FXML private TableColumn<?, ?> colResultSubject;
    @FXML private TableColumn<?, ?> colResultOwner;
    @FXML private TableColumn<?, ?> colResultPeer;
    @FXML private TableColumn<?, ?> colResultSize;
    @FXML private ProgressBar downloadProgress;
    @FXML private Label downloadStatusLabel;

    // HISTORY TAB
    @FXML private TableView<?> historyTable;
    @FXML private TableColumn<?, ?> colHistoryName;
    @FXML private TableColumn<?, ?> colHistorySourcePeer;
    @FXML private TableColumn<?, ?> colHistorySize;
    @FXML private TableColumn<?, ?> colHistoryDate;

    // COMMON
    @FXML private TabPane mainTabPane;
    @FXML private Label globalStatusLabel;

    @FXML
    public void initialize() {
        // ===== CẤU HÌNH BẢNG PEER =====
        // map tên property trong PeerInfo (getName, getIp, getPort, getStatus)
        colPeerName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPeerIp.setCellValueFactory(new PropertyValueFactory<>("ip"));
        colPeerPort.setCellValueFactory(new PropertyValueFactory<>("port"));
        colPeerStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        peerTable.setItems(peerList);

        // trạng thái chung
        if (globalStatusLabel != null) {
            globalStatusLabel.setText("Sẵn sàng");
        }
        if (peerStatusLabel != null) {
            peerStatusLabel.setText("Chưa quét peer");
        }
    }

    // ===== MENU FILE =====
    @FXML
    private void onChooseShareFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Chọn thư mục chia sẻ");
        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        var dir = chooser.showDialog(stage);
        if (dir != null) {
            shareFolderField.setText(dir.getAbsolutePath());
            globalStatusLabel.setText("Thư mục chia sẻ: " + dir.getName());
            // TODO: quét file trong thư mục và load vào sharedFileTable
        }
    }

    @FXML
    private void onExit() {
        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        stage.close();
    }

    // ===== MENU PEER / TAB PEER =====
    @FXML
    private void onScanPeers() {
        // 1. Xóa danh sách cũ
        peerList.clear();
        peerStatusLabel.setText("Đang quét peer (demo)...");

        // 2. DEMO: Thêm vài peer giả lập
        // Sau này bạn thay đoạn này bằng code quét thật trong LAN
        peerList.add(new PeerInfo("Peer A", "192.168.1.10", 5000, "Online"));
        peerList.add(new PeerInfo("Peer B", "192.168.1.11", 5000, "Online"));
        peerList.add(new PeerInfo("Peer C", "192.168.1.20", 5000, "Offline"));

        peerStatusLabel.setText("Đã tìm thấy " + peerList.size() + " peer (demo)");

        // Nếu muốn cập nhật status chung:
        globalStatusLabel.setText("Scan peer LAN (demo) xong");
    }

    @FXML
    private void onConnectPeer() {
        PeerInfo selected = peerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Kết nối peer", "Vui lòng chọn một peer trong danh sách trước.");
            return;
        }

        // TODO: kết nối thật tới selected.getIp():selected.getPort()
        showInfo("Kết nối peer",
                "Demo: Giả lập kết nối tới " + selected.getName() +
                        " (" + selected.getIp() + ":" + selected.getPort() + ")");
        peerStatusLabel.setText("Đã kết nối tới " + selected.getName());
    }

    @FXML
    private void onDisconnectPeer() {
        // TODO: ngắt kết nối thật
        peerStatusLabel.setText("Đã ngắt kết nối");
    }

    // ===== TAB SHARE =====
    @FXML
    private void onRefreshSharedFiles() {
        // TODO: quét lại thư mục shareFolderField
        showInfo("Quét tài liệu", "Đã quét lại thư mục chia sẻ (demo).");
    }

    @FXML
    private void onAddSharedFile() {
        // TODO: mở FileChooser, copy vào thư mục chia sẻ
        showInfo("Thêm tài liệu", "Demo: Thêm tài liệu mới vào danh sách chia sẻ.");
    }

    @FXML
    private void onRemoveSharedFile() {
        // TODO: xóa file được chọn khỏi share (hoặc bỏ đánh dấu chia sẻ)
        showInfo("Xóa tài liệu", "Demo: Xóa tài liệu khỏi danh sách chia sẻ.");
    }

    // ===== TAB TÌM KIẾM =====
    @FXML
    private void onSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        // TODO: gửi yêu cầu tìm kiếm đến các peer, nhận kết quả, đổ vào searchResultTable
        downloadStatusLabel.setText("Đã gửi yêu cầu tìm: " + keyword + " (demo)");
    }

    @FXML
    private void onViewFileDetails() {
        // TODO: mở hộp thoại hiển thị metadata chi tiết của file
        showInfo("Chi tiết tài liệu", "Demo: Hiển thị thông tin chi tiết tài liệu.");
    }

    @FXML
    private void onDownloadFile() {
        // TODO: gửi yêu cầu tải, cập nhật downloadProgress
        downloadProgress.setProgress(0.3);
        downloadStatusLabel.setText("Đang tải (demo)...");
        // Sau khi tải xong:
        // downloadProgress.setProgress(1.0);
        // downloadStatusLabel.setText("Hoàn tất");
    }

    // ===== TAB LỊCH SỬ =====
    @FXML
    private void onRefreshHistory() {
        // TODO: load lịch sử từ SQLite/JSON
        showInfo("Lịch sử tải", "Demo: Cập nhật lại bảng lịch sử.");
    }

    @FXML
    private void onClearHistory() {
        // TODO: xóa file/hồ sơ lịch sử
        showInfo("Lịch sử tải", "Demo: Đã xóa lịch sử (chưa thao tác thật).");
    }

    // ===== TRỢ GIÚP =====
    @FXML
    private void onAbout() {
        showInfo(
                "Giới thiệu",
                "P2P Học tập - Demo giao diện JavaFX\n" +
                        "Chức năng: kết nối peer, chia sẻ và tải tài liệu trong LAN."
        );
    }

    // ===== HÀM HỖ TRỢ =====
    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
