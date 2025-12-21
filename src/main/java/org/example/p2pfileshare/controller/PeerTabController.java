package org.example.p2pfileshare.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.network.control.ControlClient;
import org.example.p2pfileshare.service.FileShareService;
import org.example.p2pfileshare.service.PeerService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PeerTabController {

    private PeerService peerService;
    private FileShareService fileShareService;
    private ControlClient controlClient;
    private Label globalStatusLabel;

    // map lưu nhiều controller, key = peerId
    private final Map<String, ConnectedPeerController> connectedControllers = new HashMap<>();

    @FXML private TableView<PeerInfo> peerTable;
    @FXML private TableColumn<PeerInfo, String> colPeerName;
    @FXML private TableColumn<PeerInfo, String> colPeerIp;
    @FXML private TableColumn<PeerInfo, Number> colPeerPort;
    @FXML private TableColumn<PeerInfo, PeerInfo.ConnectionState> colPeerStatus;
    @FXML private Label peerStatusLabel;
    @FXML private TabPane mainTabPane; // nếu không có trong FXML, có thể set từ RootController

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
        onScanPeers();
    }

    private void setupTable() {
        colPeerName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPeerIp.setCellValueFactory(new PropertyValueFactory<>("ip"));
        colPeerPort.setCellValueFactory(new PropertyValueFactory<>("fileServerPort"));

        // Hiển thị trạng thái kết nối thực tế thay vì "Online"
        colPeerStatus.setCellValueFactory(new PropertyValueFactory<>("connectionState"));
        colPeerStatus.setCellFactory(column -> new TableCell<PeerInfo, PeerInfo.ConnectionState>() {
            @Override
            protected void updateItem(PeerInfo.ConnectionState state, boolean empty) {
                super.updateItem(state, empty);
                if (empty || state == null) {
                    setText(null);
                    setStyle("");
                } else {
                    switch (state) {
                        case NOT_CONNECTED:
                            setText("Chưa kết nối");
                            setStyle("-fx-text-fill: #666666;");
                            break;
                        case PENDING:
                            setText("Đang kết nối...");
                            setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;");
                            break;
                        case CONNECTED:
                            setText("Đã kết nối");
                            setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
                            break;
                        case REJECTED:
                            setText("Bị từ chối");
                            setStyle("-fx-text-fill: #f44336;");
                            break;
                    }
                }
            }
        });

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

        // snapshot trạng thái TRƯỚC KHI clear
        Map<String, PeerInfo.ConnectionState> prevStates = peerList.stream()
                .collect(Collectors.toMap(
                        PeerInfo::getPeerId,
                        PeerInfo::getConnectionState,
                        (a,b) -> a
                ));

        peerStatusLabel.setText("Đang quét...");
        peerTable.setDisable(true);

        Task<List<PeerInfo>> task = new Task<>() {
            @Override
            protected List<PeerInfo> call() {
                return peerService.scanPeers();
            }
        };

        task.setOnSucceeded(e -> {
            List<PeerInfo> scanned = task.getValue();

            // gán lại state
            for (PeerInfo p : scanned) {
                PeerInfo.ConnectionState prev = prevStates.get(p.getPeerId());
                if (prev == PeerInfo.ConnectionState.CONNECTED) {
                    p.setConnectionState(PeerInfo.ConnectionState.CONNECTED);
                } else {
                    p.setConnectionState(PeerInfo.ConnectionState.NOT_CONNECTED);
                }
            }

            peerList.setAll(scanned);
            peerStatusLabel.setText("Đã tìm thấy " + scanned.size() + " peer");
            if (globalStatusLabel != null) globalStatusLabel.setText("Quét LAN xong");
            peerTable.setDisable(false);
        });

        task.setOnFailed(e -> {
            peerStatusLabel.setText("Lỗi khi quét");
            peerTable.setDisable(false);
        });

        new Thread(task).start();
    }

    // Public helper để gọi từ RootController khi cần refresh
    public void refresh() {
        onScanPeers();
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
                openConnectedTab(peer);
            } else {
                peer.setConnectionState(PeerInfo.ConnectionState.REJECTED);
                peerStatusLabel.setText("Peer từ chối hoặc không phản hồi");
            }
            peerTable.refresh();
        });

        new Thread(task).start();
    }

    private void openConnectedTab(PeerInfo peer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/p2pfileshare/ConnectedPeerTab.fxml"));
            AnchorPane content = loader.load();

            ConnectedPeerController controller = loader.getController();
            controller.init(peer, controlClient, fileShareService);

            // NEW: đăng ký callback để khi tab gọi là đã ngắt kết nối thì cập nhật peerList và xoá mapping
            controller.setOnDisconnected(() -> {
                // chạy trên JavaFX thread
                Platform.runLater(() -> {
                    // Đặt trạng thái của peer trong danh sách về NOT_CONNECTED
                    for (PeerInfo p : peerList) {
                        if (peer.getPeerId().equals(p.getPeerId())) {
                            p.setConnectionState(PeerInfo.ConnectionState.NOT_CONNECTED);
                            break;
                        }
                    }
                    // Xoá controller mapping và cập nhật UI
                    connectedControllers.remove(peer.getPeerId());
                    if (peerTable != null) peerTable.refresh();
                    if (peerStatusLabel != null) peerStatusLabel.setText("Đã ngắt kết nối: " + peer.getName());
                });
            });

            Tab tab = new Tab("Kết nối: " + peer.getName());
            tab.setContent(content);
            tab.setClosable(true);

            // đăng ký controller theo peerId để có thể cập nhật sau này
            connectedControllers.put(peer.getPeerId(), controller);

            // khi tab đóng thì xoá mapping
            tab.setOnClosed(ev -> connectedControllers.remove(peer.getPeerId()));

            // tìm TabPane từ một control trong scene
            TabPane tabPane = mainTabPane;
            if (tabPane == null) {
                Node n = peerTable;
                while (n != null && !(n instanceof TabPane)) {
                    n = n.getParent();
                }
                if (n instanceof TabPane) tabPane = (TabPane) n;
            }
            if (tabPane != null) {
                tabPane.getTabs().add(tab);
                tabPane.getSelectionModel().select(tab);
            } else {
                new Alert(Alert.AlertType.WARNING, "Không tìm thấy TabPane để mở tab mới").showAndWait();
            }

        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Lỗi tải UI tab kết nối: " + ex.getMessage()).showAndWait();
        }
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

        // Nếu chưa kết nối thì chỉ cập nhật UI
        if (peer.getConnectionState() != PeerInfo.ConnectionState.CONNECTED) {
            peer.setConnectionState(PeerInfo.ConnectionState.NOT_CONNECTED);
            peerTable.refresh();
            peerStatusLabel.setText("Đã ngắt kết nối");
            return;
        }

        peerStatusLabel.setText("Đang ngắt kết nối...");
        peer.setConnectionState(PeerInfo.ConnectionState.PENDING);
        peerTable.refresh();

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                // Gọi ControlClient để gửi DISCONNECT_REQUEST tới peer
                return controlClient.sendDisconnectRequest(peer);
            }
        };

        task.setOnSucceeded(e -> {
            boolean ok = Boolean.TRUE.equals(task.getValue());
            if (ok) {
                // Thành công: cập nhật trạng thái trong peerList
                peer.setConnectionState(PeerInfo.ConnectionState.NOT_CONNECTED);
                peerStatusLabel.setText("Đã ngắt kết nối");

                // Nếu có tab kết nối đang mở, báo để cập nhật UI tab và xoá mapping controller
                ConnectedPeerController ctrl = connectedControllers.remove(peer.getPeerId());
                if (ctrl != null) {
                    ctrl.onPeerDisconnected();
                    // Không tự động đóng tab để tránh thao tác UI phức tạp ở đây; RootController/TabPane có thể đóng nếu muốn.
                }

                peerTable.refresh();
            } else {
                peer.setConnectionState(PeerInfo.ConnectionState.CONNECTED);
                peerStatusLabel.setText("Ngắt kết nối thất bại");
                new Alert(Alert.AlertType.WARNING, "Không thể gửi yêu cầu ngắt kết nối tới peer").showAndWait();
                peerTable.refresh();
            }
        });

        task.setOnFailed(e -> {
            peer.setConnectionState(PeerInfo.ConnectionState.CONNECTED);
            peerStatusLabel.setText("Lỗi khi ngắt kết nối");
            new Alert(Alert.AlertType.ERROR, "Lỗi khi thực hiện ngắt kết nối: " + task.getException()).showAndWait();
            peerTable.refresh();
        });

        new Thread(task, "disconnect-peer").start();
    }

    private void show(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    // PUBLIC API: gọi khi remote peer bị ngắt (hoặc khi muốn đặt trạng thái peer về "chưa kết nối")
    public void onRemotePeerDisconnected(String peerId) {
        if (peerId == null || peerId.isBlank()) return;

        // Cập nhật trên JavaFX thread để tránh lỗi đa luồng
        Platform.runLater(() -> {
            boolean updated = false;

            // 1) cập nhật trạng thái trong peer list
            for (PeerInfo p : peerList) {
                if (peerId.equals(p.getPeerId())) {
                    p.setConnectionState(PeerInfo.ConnectionState.NOT_CONNECTED);
                    updated = true;
                    if (peerStatusLabel != null) {
                        peerStatusLabel.setText("Peer " + p.getName() + " đã bị ngắt kết nối");
                    }
                    break;
                }
            }

            // 2) tìm controller của tab tương ứng và gọi method để cập nhật UI tab nếu tab đang mở
            ConnectedPeerController ctrl = connectedControllers.get(peerId);
            if (ctrl != null) {
                ctrl.onPeerDisconnected();
            }

            // Nếu không tìm thấy controller, không sao — peerList đã được cập nhật
            if (!updated) {
                // Optional: reload toàn bộ peers từ service nếu muốn đồng bộ
                // refresh();
            }

            if (peerTable != null) peerTable.refresh();
        });
    }
}
