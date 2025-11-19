package org.example.p2pfileshare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import org.example.p2pfileshare.service.FileShareService;
import org.example.p2pfileshare.service.HistoryService;
import org.example.p2pfileshare.service.PeerService;
import org.example.p2pfileshare.service.SearchService;

public class RootController {

    // FX Include controllers
    @FXML private PeerTabController peerTabController;
    @FXML private ShareTabController shareTabController;
    @FXML private SearchTabController searchTabController;
    @FXML private HistoryTabController historyTabController;

    // Global UI
    @FXML private TabPane mainTabPane;
    @FXML private Label globalStatusLabel;

    // Service Layer
    private PeerService peerService;
    private FileShareService fileShareService;
    private SearchService searchService;
    private HistoryService historyService;

    private String myName;

    @FXML
    public void initialize() {
        // =========================
        // 1) LẤY TÊN PEER
        // =========================
        myName = askPeerName();

        // =========================
        // 2) KHỞI TẠO SERVICES
        // =========================
        int fileServerPort = 6003;
//        PeerTCPServer tcpServer = new PeerTCPServer(fileServerPort, shareFolder);
        peerService      = new PeerService(myName, fileServerPort);
        fileShareService = new FileShareService(fileServerPort);
        searchService    = new SearchService();
        historyService   = new HistoryService();

        // =========================
        // 3) INJECT SERVICE VÀO CÁC TAB
        // =========================

        if (peerTabController != null) {
            peerTabController.init(peerService, fileShareService, globalStatusLabel);
        }

        if (shareTabController != null) {
            shareTabController.init(fileShareService, globalStatusLabel);
        }

        if (searchTabController != null) {
            searchTabController.init(searchService, fileShareService, globalStatusLabel);
        }

        if (historyTabController != null) {
            historyTabController.init(historyService, globalStatusLabel);
        }

        globalStatusLabel.setText("Sẵn sàng");
    }

    // -------------------------------
    // CHỨC NĂNG MENU TRONG MAINVIEW
    // -------------------------------

    @FXML
    private void onChooseShareFolder() {
        globalStatusLabel.setText("Hãy vào tab 'Tài liệu chia sẻ' để chọn thư mục.");
    }

    @FXML
    private void onExit() {
        mainTabPane.getScene().getWindow().hide();
    }

    @FXML
    private void onAbout() {
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);

        alert.setTitle("Giới thiệu");
        alert.setHeaderText("P2P File Sharing - JavaFX");
        alert.setContentText("Ứng dụng chia sẻ file ngang hàng trong LAN.");
        alert.showAndWait();
    }

    // -------------------------------
    // HÀM HỖ TRỢ
    // -------------------------------

    private String askPeerName() {
        TextInputDialog dialog = new TextInputDialog("Peer1");
        dialog.setTitle("Tên Peer");
        dialog.setHeaderText("Vui lòng nhập tên của bạn");
        dialog.setContentText("Peer name:");

        return dialog.showAndWait().orElse("Peer_" + System.currentTimeMillis());
    }
}
