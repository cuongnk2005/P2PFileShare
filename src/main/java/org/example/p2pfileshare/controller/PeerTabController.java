package org.example.p2pfileshare.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.service.FileShareService;
import org.example.p2pfileshare.service.PeerService;

import java.nio.file.Path;
import java.util.List;

public class PeerTabController {

    // Inject từ RootController
    private PeerService peerService;
    private FileShareService fileShareService;
    private Label globalStatusLabel;

    // UI
    @FXML private TableView<PeerInfo> peerTable;
    @FXML private TableColumn<PeerInfo, String> colPeerName;
    @FXML private TableColumn<PeerInfo, String> colPeerIp;
    @FXML private TableColumn<PeerInfo, Number> colPeerPort;
    @FXML private TableColumn<PeerInfo, String> colPeerStatus;
    @FXML private Label peerStatusLabel;

    @FXML private ProgressBar downloadProgress;
    @FXML private Label downloadStatusLabel;

    private final ObservableList<PeerInfo> peerList = FXCollections.observableArrayList();

    // Gọi từ RootController để inject service
    public void init(PeerService peerService,
                     FileShareService fileShareService,
                     Label globalStatusLabel) {

        this.peerService = peerService;
        this.fileShareService = fileShareService;
        this.globalStatusLabel = globalStatusLabel;

        setupPeerTable();
    }

    private void setupPeerTable() {
        colPeerName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPeerIp.setCellValueFactory(new PropertyValueFactory<>("ip"));
        colPeerPort.setCellValueFactory(new PropertyValueFactory<>("port"));
        colPeerStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        peerTable.setItems(peerList);
    }

    // ============= BUTTON ACTIONS =============

    @FXML
    private void onScanPeers() {
        peerList.clear();
        peerStatusLabel.setText("Đang quét peer...");
        peerTable.setDisable(true);

        Task<List<PeerInfo>> task = new Task<>() {
            @Override
            protected List<PeerInfo> call() {
                return peerService.scanPeers();
            }
        };

        task.setOnSucceeded(e -> {
            peerList.setAll(task.getValue());
            peerStatusLabel.setText("Đã tìm thấy " + task.getValue().size() + " peer");
            peerTable.setDisable(false);
            globalStatusLabel.setText("Quét LAN xong");
        });

        task.setOnFailed(e -> {
            peerStatusLabel.setText("Lỗi khi quét peer");
            peerTable.setDisable(false);
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    @FXML
    private void onConnectPeer() {
        PeerInfo peer = peerTable.getSelectionModel().getSelectedItem();
        if (peer == null) {
            showMsg("Chưa chọn peer để kết nối");
            return;
        }

        showMsg("Giả lập kết nối đến " + peer.getName() +
                " (" + peer.getIp() + ":" + peer.getPort() + ")");
        peerStatusLabel.setText("Đã kết nối tới " + peer.getName());
    }

    @FXML
    private void onDisconnectPeer() {
        peerStatusLabel.setText("Đã ngắt kết nối");
    }

    @FXML
    private void onDownloadFile() {
        PeerInfo peer = peerTable.getSelectionModel().getSelectedItem();
        if (peer == null) {
            showMsg("Chưa chọn peer!");
            return;
        }

        String fileName = "test.pdf"; // Sau này lấy từ bảng search
        Path saveTo = Path.of("Downloads/" + fileName);

        downloadProgress.setProgress(0);
        downloadStatusLabel.setText("Đang tải...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return fileShareService.download(peer, fileName, saveTo);
            }
        };

        task.setOnSucceeded(e -> {
            boolean ok = task.getValue();
            downloadProgress.setProgress(ok ? 1 : 0);
            downloadStatusLabel.setText(ok ? "Hoàn tất!" : "Tải lỗi!");
        });

        new Thread(task).start();
    }

    private void showMsg(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.showAndWait();
    }
}
