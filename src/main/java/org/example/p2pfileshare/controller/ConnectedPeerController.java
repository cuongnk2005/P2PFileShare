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

import java.nio.file.Path;
import java.util.List;

public class ConnectedPeerController {

    @FXML private Label peerNameLabel;
    @FXML private TableView<Row> fileTable;
    @FXML private TableColumn<Row, String> colName;
    @FXML private TableColumn<Row, String> colRelative;
    @FXML private TableColumn<Row, Long>   colSize;
    @FXML private ProgressBar progress;
    @FXML private Label statusLabel;

    private final ObservableList<Row> rows = FXCollections.observableArrayList();

    private PeerInfo peer;
    private ControlClient controlClient;
    private FileShareService fileShareService;

    public void init(PeerInfo peer, ControlClient controlClient, FileShareService fileShareService) {
        this.peer = peer;
        this.controlClient = controlClient;
        this.fileShareService = fileShareService;

        peerNameLabel.setText(peer.getName() + " (" + peer.getIp() + ")");

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRelative.setCellValueFactory(new PropertyValueFactory<>("relativePath"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        fileTable.setItems(rows);

        // nạp lần đầu
        reload();
    }

    @FXML
    private void onReload() {
        reload();
    }

    private void reload() {
        statusLabel.setText("Đang tải danh sách...");
        Task<List<ControlClient.RemoteFile>> task = new Task<>() {
            @Override
            protected List<ControlClient.RemoteFile> call() {
                return controlClient.listFiles(peer);
            }
        };

        task.setOnSucceeded(e -> {
            rows.clear();
            for (var rf : task.getValue()) {
                rows.add(new Row(rf.name, rf.relativePath, rf.size));
            }
            statusLabel.setText("Đã nạp " + rows.size() + " file");
        });
        task.setOnFailed(e -> statusLabel.setText("Lỗi tải danh sách"));

        new Thread(task, "reload-remote-files").start();
    }

    @FXML
    private void onDownloadSelected() {
        Row sel = fileTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.INFORMATION, "Hãy chọn 1 file để tải").showAndWait();
            return;
        }

        progress.setProgress(0);
        statusLabel.setText("Đang tải: " + sel.name);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                Path saveTo = Path.of("Downloads").resolve(sel.name);
                return fileShareService.download(peer, sel.relativePath, saveTo);
            }
        };

        task.setOnSucceeded(e -> {
            boolean ok = task.getValue();
            progress.setProgress(ok ? 1.0 : 0.0);
            statusLabel.setText(ok ? "Hoàn tất" : "Lỗi tải");
        });
        task.setOnFailed(e -> {
            progress.setProgress(0);
            statusLabel.setText("Lỗi tải");
        });

        new Thread(task, "download-remote-file").start();
    }

    @FXML
    private void onDisconnect() {
        // chỉ đổi trạng thái UI; logic disconnect nâng cao có thể thêm sau
        statusLabel.setText("Đã ngắt kết nối");
        // đóng tab hiện tại
        peerNameLabel.getScene().getWindow();
        // Tab sẽ do RootController tạo và chứa, ở đây controller con không tự đóng.
    }

    // Row model cho TableView
    public static class Row {
        private final String name;
        private final String relativePath;
        private final long size;
        public Row(String name, String relativePath, long size) {
            this.name = name; this.relativePath = relativePath; this.size = size;
        }
        public String getName() { return name; }
        public String getRelativePath() { return relativePath; }
        public long getSize() { return size; }
    }
}

