package org.example.p2pfileshare.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.network.control.ControlClient;
import org.example.p2pfileshare.service.FileShareService;
import org.example.p2pfileshare.service.PeerService;

import java.nio.file.Path;
import java.util.List;

public class PeerTabController {

    private PeerService peerService;
    private FileShareService fileShareService;
    private ControlClient controlClient;
    private Label globalStatusLabel;

    @FXML private TableView<PeerInfo> peerTable;
    @FXML private TableColumn<PeerInfo, String> colPeerName;
    @FXML private TableColumn<PeerInfo, String> colPeerIp;
    @FXML private TableColumn<PeerInfo, Number> colPeerPort;
    @FXML private TableColumn<PeerInfo, String> colPeerStatus;
    @FXML private Label peerStatusLabel;

    @FXML private ProgressBar downloadProgress;
    @FXML private Label downloadStatusLabel;

    private final ObservableList<PeerInfo> peerList = FXCollections.observableArrayList();

    public void init(PeerService peerService,
                     FileShareService fileShareService,
                     ControlClient controlClient,
                     Label globalStatusLabel) {

        this.peerService = peerService;
        this.fileShareService = fileShareService;
        this.controlClient = controlClient;
        this.globalStatusLabel = globalStatusLabel;

        setupTable();
    }

    private void setupTable() {
        colPeerName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPeerIp.setCellValueFactory(new PropertyValueFactory<>("ip"));
        colPeerPort.setCellValueFactory(new PropertyValueFactory<>("fileServerPort"));
        colPeerStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        peerTable.setItems(peerList);
    }

    private void showMsg(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }


    // ===================================
    // QUÉT PEER
    // ===================================
    @FXML
    private void onScanPeers() {
        peerList.clear();
        peerStatusLabel.setText("Đang quét...");
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
            globalStatusLabel.setText("Quét LAN xong");
            peerTable.setDisable(false);
        });

        task.setOnFailed(e -> {
            peerStatusLabel.setText("Lỗi khi quét");
            peerTable.setDisable(false);
        });

        new Thread(task).start();
    }


    // ===================================
    // KẾT NỐI PEER
    // ===================================
    @FXML
    private void onConnectPeer() {
        PeerInfo peer = peerTable.getSelectionModel().getSelectedItem();
        if (peer == null) {
            show("Chưa chọn peer!");
            return;
        }

        peerStatusLabel.setText("Đang gửi CONNECT_REQUEST...");
        peer.setConnectionState(PeerInfo.ConnectionState.PENDING);
        peerTable.refresh();

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return controlClient.sendConnectRequest(peer);
            }
        };

        task.setOnSucceeded(e -> {
            boolean ok = task.getValue();

            if (ok) {
                peer.setConnectionState(PeerInfo.ConnectionState.CONNECTED);
                peerStatusLabel.setText("Kết nối thành công!");
            } else {
                peer.setConnectionState(PeerInfo.ConnectionState.REJECTED);
                peerStatusLabel.setText("Peer từ chối hoặc không phản hồi");
            }
            peerTable.refresh();
        });

        new Thread(task).start();
    }

    @FXML
    private void onDownloadFile() {
        PeerInfo peer = peerTable.getSelectionModel().getSelectedItem();

        // 1) Chưa chọn peer
        if (peer == null) {
            showMsg("Vui lòng chọn peer trước!");
            return;
        }

        // 2) Peer chưa kết nối
        if (peer.getConnectionState() != PeerInfo.ConnectionState.CONNECTED) {
            showMsg("Bạn phải kết nối với peer trước khi tải file!");
            return;
        }

        // ======= DEMO TẢI FILE TEST =======
        String fileName = "test.pdf";
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
            downloadProgress.setProgress(ok ? 1.0 : 0.0);
            downloadStatusLabel.setText(ok ? "Hoàn tất!" : "Lỗi tải file!");
        });

        task.setOnFailed(e -> {
            downloadProgress.setProgress(0);
            downloadStatusLabel.setText("Lỗi tải file!");
            task.getException().printStackTrace();
        });

        new Thread(task, "peer-download").start();
    }


    // ===================================
    // NGẮT KẾT NỐI
    // ===================================
    @FXML
    private void onDisconnectPeer() {
        PeerInfo peer = peerTable.getSelectionModel().getSelectedItem();
        if (peer == null) {
            show("Hãy chọn peer trước!");
            return;
        }

        peer.setConnectionState(PeerInfo.ConnectionState.NOT_CONNECTED);
        peerTable.refresh();
        peerStatusLabel.setText("Đã ngắt kết nối");
    }

    private void show(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}
