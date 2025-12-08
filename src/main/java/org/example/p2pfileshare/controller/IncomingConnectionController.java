package org.example.p2pfileshare.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.network.control.ControlServer;
import org.example.p2pfileshare.service.PeerService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class IncomingConnectionController {

    @FXML private TableView<PeerInfo> connectedPeerTable;
    @FXML private TableColumn<PeerInfo, String> colPeerId;
    @FXML private TableColumn<PeerInfo, String> colDisplayName;
    @FXML private TableColumn<PeerInfo, String> colIp;
    @FXML private TableColumn<PeerInfo, String> colConnectTime;
    @FXML private Label statusLabel;
    @FXML private Button refreshButton;
    @FXML private Button disconnectButton;

    private PeerService peerService;
    private ControlServer controlServer;
    private Label globalStatusLabel;

    private final ObservableList<PeerInfo> incomingPeerList = FXCollections.observableArrayList();

    public void init(PeerService peerService, ControlServer controlServer, Label globalStatusLabel) {
        this.peerService = peerService;
        this.controlServer = controlServer;
        this.globalStatusLabel = globalStatusLabel;

        setupTable();
        loadIncomingConnections();
    }

    private void setupTable() {
        // Setup các cột
        colPeerId.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getPeerId()));

        colDisplayName.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getName()));

        colIp.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getIp()));

        colConnectTime.setCellValueFactory(data ->
            new SimpleStringProperty(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"))));

        connectedPeerTable.setItems(incomingPeerList);
    }

    private void loadIncomingConnections() {
        // Lấy tất cả peers đã được phát hiện
        List<PeerInfo> allPeers = peerService.getListPeer();

        // Lọc ra những peer đã được chấp nhận kết nối
        List<PeerInfo> acceptedPeers = controlServer.getAcceptedPeers(allPeers);

        incomingPeerList.setAll(acceptedPeers);
        statusLabel.setText("Có " + acceptedPeers.size() + " peer đang kết nối đến");

        if (globalStatusLabel != null) {
            globalStatusLabel.setText("Đã tải danh sách peer kết nối đến");
        }
    }

    @FXML
    private void onRefresh() {
        statusLabel.setText("Đang làm mới...");
        loadIncomingConnections();
    }

    @FXML
    private void onDisconnect() {
        PeerInfo selected = connectedPeerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cảnh báo");
            alert.setHeaderText("Chưa chọn peer");
            alert.setContentText("Vui lòng chọn peer để ngắt kết nối");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText("Ngắt kết nối với peer");
        confirm.setContentText("Bạn có chắc muốn ngắt kết nối với " + selected.getName() + "?");

        var result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // TODO: Implement disconnect logic
            incomingPeerList.remove(selected);
            statusLabel.setText("Đã ngắt kết nối với " + selected.getName());
        }
    }
}