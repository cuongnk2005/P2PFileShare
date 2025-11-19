package org.example.p2pfileshare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.example.p2pfileshare.service.FileShareService;

import java.io.File;

public class ShareTabController {

    private FileShareService fileShareService;
    private Label globalStatusLabel;

    @FXML private TextField shareFolderField;
    @FXML private TableView<?> sharedFileTable;

    // Inject từ RootController
    public void init(FileShareService fileShareService, Label globalStatusLabel) {
        this.fileShareService = fileShareService;
        this.globalStatusLabel = globalStatusLabel;
    }

    @FXML
    private void onChooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Chọn thư mục chia sẻ");

        Stage stage = (Stage) shareFolderField.getScene().getWindow();
        File dir = chooser.showDialog(stage);

        if (dir != null) {
            shareFolderField.setText(dir.getAbsolutePath());
            fileShareService.startSharing(dir);  // <-- start luôn FileServer
            globalStatusLabel.setText("Đang chia sẻ từ: " + dir.getName());
        }
    }

    @FXML
    private void onRefreshSharedFiles() {
        globalStatusLabel.setText("Đã quét thư mục chia sẻ (demo)");
    }

    @FXML
    private void onAddSharedFile() {
        globalStatusLabel.setText("Demo: thêm file chia sẻ");
    }

    @FXML
    private void onRemoveSharedFile() {
        globalStatusLabel.setText("Demo: xóa file chia sẻ");
    }
}
