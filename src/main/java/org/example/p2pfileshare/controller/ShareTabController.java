package org.example.p2pfileshare.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.example.p2pfileshare.service.FileShareService;

import java.io.File;
import java.util.List;

public class ShareTabController {

    private FileShareService fileShareService;
    private Label globalStatusLabel;

    @FXML private TextField shareFolderField;
    @FXML private TableView<File> sharedFileTable;
    @FXML private TableColumn<File, String> colSharedName;
    @FXML private TableColumn<File, String> colSharedType;
    @FXML private TableColumn<File, Long>   colSharedSize;

    private final ObservableList<File> sharedFiles = FXCollections.observableArrayList();

    public void init(FileShareService fileShareService, Label globalStatusLabel) {
        this.fileShareService = fileShareService;
        this.globalStatusLabel = globalStatusLabel;

        setupTable();
    }

    private void setupTable() {
        colSharedName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colSharedType.setCellValueFactory(c -> {
            String name = c.getValue().getName();
            int idx = name.lastIndexOf('.');
            String ext = (idx >= 0) ? name.substring(idx + 1) : "";
            return new javafx.beans.property.SimpleStringProperty(ext);
        });
        colSharedSize.setCellValueFactory(c ->
                new javafx.beans.property.SimpleLongProperty(c.getValue().length()).asObject()
        );
        sharedFileTable.setItems(sharedFiles);
    }

    @FXML
    private void onChooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Chọn thư mục chia sẻ");
        Stage stage = (Stage) shareFolderField.getScene().getWindow();
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            shareFolderField.setText(dir.getAbsolutePath());
            fileShareService.setShareFolder(dir);
            refreshSharedFiles();
            if (globalStatusLabel != null) {
                globalStatusLabel.setText("Thư mục chia sẻ: " + dir.getName());
            }
        }
    }

    @FXML
    private void onRefreshSharedFiles() {
        refreshSharedFiles();
    }

    private void refreshSharedFiles() {
        List<File> list = fileShareService.listSharedFiles();
        sharedFiles.setAll(list);
    }

    @FXML
    private void onAddSharedFile() {
        // TODO: copy file vào thư mục chia sẻ (tạm thời để trống)
        Alert a = new Alert(Alert.AlertType.INFORMATION, "Demo: thêm file vào thư mục chia sẻ bằng cách copy thủ công.");
        a.showAndWait();
    }

    @FXML
    private void onRemoveSharedFile() {
        File selected = sharedFileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Chưa chọn file để xóa.");
            a.showAndWait();
            return;
        }
        // TODO: xoá file hoặc đổi trạng thái "không chia sẻ nữa"
        Alert a = new Alert(Alert.AlertType.INFORMATION, "Demo: chưa xoá file thật, chỉ minh hoạ.");
        a.showAndWait();
    }
}
