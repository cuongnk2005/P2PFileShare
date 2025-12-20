package org.example.p2pfileshare.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.network.control.ControlClient;
import org.example.p2pfileshare.service.FileShareService;
import org.example.p2pfileshare.util.AppConfig;

import java.io.File;
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
        // Format hiển thị kích thước: KB/MB/GB
        colSize.setCellFactory(col -> new TableCell<Row, Long>() {
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
        fileTable.setItems(rows);

        // Thêm context menu chuột phải
        setupContextMenu();

        // nạp lần đầu
        reload();
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem downloadItem = new MenuItem("Tải xuống");
        downloadItem.setOnAction(e -> {
            Row selected = fileTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                downloadFile(selected);
            }
        });

        contextMenu.getItems().add(downloadItem);

        // Chỉ hiển thị menu khi có item được chọn
        fileTable.setContextMenu(contextMenu);

        // Hoặc có thể hiển thị menu chỉ khi chuột phải vào row có dữ liệu
        fileTable.setRowFactory(tv -> {
            TableRow<Row> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    fileTable.getSelectionModel().select(row.getItem());
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
    }

    @FXML
    public void onReload() {
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
        downloadFile(sel);
    }

    private void downloadFile(Row fileRow) {
        // Người dùng chọn THƯ MỤC lưu (nhớ thư mục lần trước)
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Chọn thư mục lưu file");

        // Load thư mục đã chọn lần trước nếu có
        final String KEY_LAST_DOWNLOAD_DIR = "last_download_dir";
        String last = AppConfig.load(KEY_LAST_DOWNLOAD_DIR);
        if (last != null) {
            File lastDir = new File(last);
            if (lastDir.isDirectory()) {
                dirChooser.setInitialDirectory(lastDir);
            }
        }

        File selectedDir = dirChooser.showDialog(peerNameLabel.getScene().getWindow());

        // Nếu người dùng hủy chọn, không làm gì cả
        if (selectedDir == null) {
            statusLabel.setText("Đã hủy tải xuống");
            return;
        }

        // Lưu lại thư mục vừa chọn để lần sau mở đúng chỗ
        AppConfig.save(KEY_LAST_DOWNLOAD_DIR, selectedDir.getAbsolutePath());

        // Tạo đường dẫn file đích bên trong thư mục vừa chọn, giữ nguyên tên file gốc
        File selectedFile = new File(selectedDir, fileRow.name);

        progress.setProgress(0);
        statusLabel.setText("Đang tải: " + fileRow.name);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                Path saveTo = selectedFile.toPath();
                return fileShareService.download(peer, fileRow.relativePath, saveTo);
            }
        };

        task.setOnSucceeded(e -> {
            Boolean result = task.getValue();
            System.out.println("Task getValue(): " + result);
            boolean ok = (result != null && result);
            progress.setProgress(ok ? 1.0 : 0.0);
            statusLabel.setText(ok ? "Hoàn tất - Đã lưu tại: " + selectedFile.getAbsolutePath() : "Lỗi tải");
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

    // Helper: đổi bytes -> KB/MB/GB theo ngưỡng
    private static String formatSize(long bytes) {
        final double KB = 1024.0;
        final double MB = KB * 1024.0;
        final double GB = MB * 1024.0;
        if (bytes >= GB) {
            return String.format("%.2f GB", bytes / GB);
        } else if (bytes >= MB) {
            return String.format("%.2f MB", bytes / MB);
        } else {
            return String.format("%.2f KB", bytes / KB);
        }
    }

}
