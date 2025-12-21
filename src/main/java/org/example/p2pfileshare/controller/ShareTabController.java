package org.example.p2pfileshare.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.example.p2pfileshare.model.SharedFileLocal;
import org.example.p2pfileshare.service.FileShareService;
import org.example.p2pfileshare.util.AppConfig;

import java.io.File;
import java.util.List;

public class ShareTabController {

    private FileShareService fileShareService;
    private Label globalStatusLabel;

    private static final String KEY_SHARE_DIR = "shared_folder";

    @FXML private TextField shareFolderField;

    // =======================
    // TableView dùng SharedFileLocal
    // =======================
    @FXML private TableView<SharedFileLocal> sharedFileTable;
    @FXML private TableColumn<SharedFileLocal, String> colSharedName;
    @FXML private TableColumn<SharedFileLocal, String> colSharedType;
    @FXML private TableColumn<SharedFileLocal, Long>   colSharedSize;
    @FXML private TableColumn<SharedFileLocal, String> colSharedSubject;
    @FXML private TableColumn<SharedFileLocal, String> colSharedTags;
    @FXML private TableColumn<SharedFileLocal, Boolean> colSharedVisibility;

    private final ObservableList<SharedFileLocal> sharedFiles =
            FXCollections.observableArrayList();

    public void init(FileShareService fileShareService, Label globalStatusLabel) {
        this.fileShareService = fileShareService;
        this.globalStatusLabel = globalStatusLabel;

        setupTable();
        loadLastSharedFolder();
    }

    // ánh xạ data từ file sharefilelocal lên bảng
    private void setupTable() {
        colSharedName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        colSharedType.setCellValueFactory(new PropertyValueFactory<>("extension"));
        colSharedSize.setCellValueFactory(new PropertyValueFactory<>("size"));

        // Format hiển thị kích thước: KB/MB/GB
        colSharedSize.setCellFactory(col -> new TableCell<SharedFileLocal, Long>() {
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

        colSharedSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colSharedTags.setCellValueFactory(new PropertyValueFactory<>("tags"));
        colSharedVisibility.setCellValueFactory(new PropertyValueFactory<>("visible"));
        sharedFileTable.setItems(sharedFiles);
    }

    // =============================================
    // Load thư mục chia sẻ đã lưu trong AppConfig
    // =============================================
    private void loadLastSharedFolder() {
        String last = AppConfig.load(KEY_SHARE_DIR);
        if (last != null) {
            File dir = new File(last);
            if (dir.isDirectory()) {
                applyShareFolder(dir);
            }
        }
    }

    private void applyShareFolder(File dir) {
        shareFolderField.setText(dir.getAbsolutePath());
        fileShareService.setShareFolder(dir);
        refreshSharedFiles();

        if (globalStatusLabel != null) {
            globalStatusLabel.setText("Thư mục chia sẻ: " + dir.getName());
        }
    }

    // ==========================
    // Chọn thư mục chia sẻ
    // ==========================
    @FXML
    private void onChooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Chọn thư mục chia sẻ");

        String last = AppConfig.load(KEY_SHARE_DIR);
        if (last != null) {
            File prev = new File(last);
            if (prev.isDirectory()) chooser.setInitialDirectory(prev);
        }

        Stage stage = (Stage) shareFolderField.getScene().getWindow();
        File dir = chooser.showDialog(stage);

        if (dir != null) {
            shareFolderField.setText(dir.getAbsolutePath());
            AppConfig.save(KEY_SHARE_DIR, dir.getAbsolutePath());
            fileShareService.setShareFolder(dir);

            refreshSharedFiles();

            if (globalStatusLabel != null) {
                globalStatusLabel.setText("Thư mục chia sẻ: " + dir.getName());
            }
        }
    }

    // ==========================
    // Refresh lại bảng file
    // ==========================
    @FXML
    private void onRefreshSharedFiles() {
        refreshSharedFiles();
    }

    private void refreshSharedFiles() {
        List<SharedFileLocal> list = fileShareService.listSharedFiles();
        sharedFiles.setAll(list);
    }

    // ==========================
    // Chưa implement add/remove
    // ==========================
    @FXML
    private void onAddSharedFile() {
        Alert a = new Alert(Alert.AlertType.INFORMATION,
                "Demo: thêm file vào thư mục chia sẻ bằng cách copy thủ công.");
        a.showAndWait();
    }

    @FXML
    private void onRemoveSharedFile() {
        SharedFileLocal selected = sharedFileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Chưa chọn file để xóa.");
            a.showAndWait();
            return;
        }

        Alert a = new Alert(Alert.AlertType.INFORMATION,
                "Demo: chưa xoá file thật, chỉ minh hoạ.");
        a.showAndWait();
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
