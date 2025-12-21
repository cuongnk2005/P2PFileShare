package org.example.p2pfileshare.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.network.control.ControlClient;
import org.example.p2pfileshare.network.control.ControlServer;
import org.example.p2pfileshare.network.discovery.PeerDiscovery;
import org.example.p2pfileshare.service.FileShareService;
import org.example.p2pfileshare.service.HistoryService;
import org.example.p2pfileshare.service.PeerService;
import org.example.p2pfileshare.service.SearchService;
import org.example.p2pfileshare.util.AppConfig;

import java.util.Random;
import java.util.UUID;

public class RootController {

    // Include các tab con
    @FXML private PeerTabController peerTabController;
    @FXML private ShareTabController shareTabController;
    @FXML private SearchTabController searchTabController;
    @FXML private HistoryTabController historyTabController;
    @FXML private IncomingConnectionController incomingConnectionTabController;

    @FXML private TabPane mainTabPane;
    @FXML private Label globalStatusLabel;
    @FXML private Label userNameLabel; // hiển thị tên người dùng trên status bar

    // Services
    private PeerService peerService;
    private FileShareService fileShareService;
    private SearchService searchService;
    private HistoryService historyService;

    // Control channel
    private ControlServer controlServer;
    private ControlClient controlClient;

    // Peer info
    private String myPeerId;
    private String myName; // displayName
    private final int FILE_PORT      = 6000  + new Random().nextInt(1000);
    private final int CONTROL_PORT   = 7000  + new Random().nextInt(1000);
    private static final String KEY_PEER_NAME = "peer_display_name";
    @FXML
    public void initialize() {

        // 1) Hỏi tên peer
        myName = loadOrAskPeerName();
        myPeerId = UUID.randomUUID().toString();
        historyService   = new HistoryService();
        // 2) Khởi tạo service
        peerService      = new PeerService(myPeerId, myName, FILE_PORT, CONTROL_PORT);
        fileShareService = new FileShareService(FILE_PORT, historyService);
        fileShareService.setMyDisplayName(myName); // Truyền tên hiển thị vào FileShareService
        searchService    = new SearchService();


        // 3) ControlClient để gửi request CONNECT (gửi peerId, displayName)
        controlClient = new ControlClient(myPeerId, myName);

        // 4) ControlServer để nhận CONNECT_REQUEST
        controlServer = new ControlServer(CONTROL_PORT, fromPeer -> {
            // fromPeer là peerId của peer gửi yêu cầu
            // Hỏi người dùng bằng JavaFX, nhưng phải block đến khi họ chọn xong
            java.util.concurrent.atomic.AtomicBoolean accepted = new java.util.concurrent.atomic.AtomicBoolean(false);
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Yêu cầu kết nối");
                alert.setHeaderText("Peer " + fromPeer + " muốn kết nối với bạn");
                alert.setContentText("Bạn có đồng ý không?");

                var result = alert.showAndWait();
                boolean ok = result.isPresent() && result.get().getButtonData().isDefaultButton();
                accepted.set(ok);
                latch.countDown();
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return accepted.get();
        });
        // inject FileShareService để phục vụ LIST_FILES
        controlServer.setFileShareService(fileShareService);
        controlServer.start();

        // NEW: khi ControlServer nhận DISCONNECT_NOTIFY từ remote, hiển thị Alert cho người dùng
        controlServer.setOnDisconnectNotify(msg -> {
            Platform.runLater(() -> {
                System.out.println("DISCONNECT_NOTIFY from=" + msg.fromPeer + " to=" + msg.toPeer);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Ngắt kết nối");
                alert.setHeaderText("Bạn đã bị ngắt kết nối");
                String content = (msg.note != null && !msg.note.isBlank())
                        ? msg.note
                        : ("Bạn đã bị ngắt kết nối bởi " + (msg.fromPeer != null ? msg.fromPeer : "Unknown"));
                alert.setContentText(content);
                alert.showAndWait();
                // Cập nhật global status nếu cần
                if (globalStatusLabel != null) {
                    globalStatusLabel.setText("Bạn đã bị ngắt kết nối: " + (msg.fromPeer != null ? msg.fromPeer : "Unknown"));
                }
                this.peerTabController.onRemotePeerDisconnected(msg.fromPeer);

            });
        });
//        changeNameController.setonUpdatePeerName(msg -> {
//            Platform.runLater(() -> {
//               this.myName = msg;
//            });
//        });

        System.out.println("[Root] ControlServer started at port " + CONTROL_PORT);

        // 5) Bật Discovery Responder
        PeerDiscovery.startResponder(
                myPeerId,
                myName,
                FILE_PORT,
                CONTROL_PORT
        );


        // 6) Inject service vào UI controllers
        if (peerTabController != null)
            peerTabController.init(peerService, fileShareService, controlClient, controlServer, globalStatusLabel);

        if (shareTabController != null)
            shareTabController.init(fileShareService, globalStatusLabel);

        if (searchTabController != null)
            searchTabController.init(searchService, fileShareService, controlClient, globalStatusLabel);

        if (historyTabController != null)
            historyTabController.init(historyService, globalStatusLabel);

        if (incomingConnectionTabController != null)
            incomingConnectionTabController.init(peerService, controlServer, globalStatusLabel,this.myPeerId);

        globalStatusLabel.setText("Sẵn sàng");
        // Hiển thị tên người dùng lên status bar (nếu Label đã được inject)
        if (userNameLabel != null && myName != null) {
            userNameLabel.setText(myName);
        }
        this.fileShareService.startServer();
    }

    // ================= MENU =================

    @FXML
    private void onChooseShareFolder() {
        globalStatusLabel.setText("Hãy vào tab 'Tài liệu chia sẻ' để chọn thư mục.");
        mainTabPane.getSelectionModel().select(1);
    }

    @FXML
    private void onExit() {
        mainTabPane.getScene().getWindow().hide();
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Giới thiệu");
        alert.setHeaderText("P2P File Sharing - JavaFX");
        alert.setContentText("Ứng dụng chia sẻ file ngang hàng trong LAN.");
        alert.showAndWait();
    }

    @FXML
    private void onChangeName() {
        var owner = mainTabPane != null && mainTabPane.getScene() != null ? mainTabPane.getScene().getWindow() : null;
        var opt = ChangeNameController.showDialog(
                mainTabPane.getScene().getWindow(),
                myName,
                controlClient,
                controlServer,
                newName -> {
                    myName = newName;
                    userNameLabel.setText(myName);
                    peerService.setMyDisplayName(myName);
                    fileShareService.setMyDisplayName(myName);
                    controlClient.setMyDisplayName(myName);
                }
        );
        if (opt.isPresent()) {
            String newName = opt.get().trim();
            if (!newName.isEmpty() && !newName.equals(myName)) {
                // 1) Lưu config
                AppConfig.save(KEY_PEER_NAME, newName);

                // 2) Cập nhật biến và UI
                myName = newName;
                if (userNameLabel != null) userNameLabel.setText(myName);

                // 3) Cập nhật service/client
                if (peerService != null) peerService.setMyDisplayName(myName);
                if (controlClient != null) controlClient.setMyDisplayName(myName);

                // 4) Refresh UI (quét lại peer list để cập nhật hiển thị nếu cần)
                if (peerTabController != null) peerTabController.refresh();

                globalStatusLabel.setText("Đã đổi tên thành: " + myName);
            }
        }
    }

    // ================= HỖ TRỢ =================

    private String loadOrAskPeerName() {
        // 1) Load tên đã lưu
        String saved = AppConfig.load(KEY_PEER_NAME);
        if (saved != null && !saved.isBlank()) {
            return saved; // ✔ Có tên rồi → dùng luôn
        }

        // 2) Chưa có → hỏi tên người dùng
        TextInputDialog dialog = new TextInputDialog("Peer1");
        dialog.setTitle("Tên Peer");
        dialog.setHeaderText("Nhập tên Peer:");
        dialog.setContentText("Tên:");

        String name = dialog.showAndWait().orElse("Peer_" + System.currentTimeMillis());

        // 3) Lưu lại để lần sau khỏi hỏi
        AppConfig.save(KEY_PEER_NAME, name);

        return name;
    }
}
