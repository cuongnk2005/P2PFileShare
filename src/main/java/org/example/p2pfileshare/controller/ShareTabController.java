package org.example.p2pfileshare.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.p2pfileshare.model.SharedFileLocal;
import org.example.p2pfileshare.network.control.ControlClient;
import org.example.p2pfileshare.service.DocumentSummaryService;
import org.example.p2pfileshare.service.FileShareService;
import org.example.p2pfileshare.util.AppConfig;
import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.service.PeerService;

import java.io.File;
import java.util.List;
import java.io.IOException;

public class ShareTabController {

    private FileShareService fileShareService;
    private Label globalStatusLabel;

    private static final String KEY_SHARE_DIR = "shared_folder";

    @FXML private TextField shareFolderField;

    // TableView dùng SharedFileLocal
    @FXML private TableView<SharedFileLocal> sharedFileTable;
    @FXML private TableColumn<SharedFileLocal, String> colSharedName;
    @FXML private TableColumn<SharedFileLocal, String> colSharedType;
    @FXML private TableColumn<SharedFileLocal, Long>   colSharedSize;
    @FXML private TableColumn<SharedFileLocal, String> colSharedSubject;
    @FXML private TableColumn<SharedFileLocal, String> colSharedTags;
    @FXML private TableColumn<SharedFileLocal, Boolean> colSharedVisibility;

    private final ObservableList<SharedFileLocal> sharedFiles =
            FXCollections.observableArrayList();

    private PeerTabController peerTabController;
    private ControlClient controlClient;
    private PeerService peerService;
    private final DocumentSummaryService documentSummaryService = new DocumentSummaryService();

    public void init(FileShareService fileShareService, Label globalStatusLabel, ControlClient controlClient, PeerTabController peerTabController) {
        this.fileShareService = fileShareService;
        this.globalStatusLabel = globalStatusLabel;
        this.controlClient = controlClient;
        this.peerTabController = peerTabController;

        setupTable();
        loadLastSharedFolder();
    }

    // ánh xạ data từ file sharefilelocal lên bảng
    private void setupTable() {
        colSharedName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        colSharedType.setCellValueFactory(new PropertyValueFactory<>("extension"));
        colSharedSize.setCellValueFactory(new PropertyValueFactory<>("size"));

        // Format hiển thị kích thước file
        colSharedSize.setCellFactory(col -> new TableCell<SharedFileLocal, Long>() { // chặn data trước để format
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
        if (colSharedTags != null) {
            colSharedTags.setCellValueFactory(new PropertyValueFactory<>("tag"));
        }
        colSharedVisibility.setCellValueFactory(new PropertyValueFactory<>("visible"));
        sharedFileTable.setItems(sharedFiles);

        sharedFileTable.setRowFactory(tv -> {
            TableRow<SharedFileLocal> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();

            MenuItem summarizeItem = new MenuItem("✨ Tóm tắt nội dung (AI)");
            summarizeItem.setStyle("-fx-font-weight: bold;");

            summarizeItem.setOnAction(event -> {
                SharedFileLocal selected = row.getItem();
                if (selected != null) {
                    onSummarizeFile(selected);
                }
            });

            MenuItem deleteItem = new MenuItem("🗑 Xóa file");
            deleteItem.setOnAction(event -> {
                sharedFileTable.getSelectionModel().select(row.getItem());
                onRemoveSharedFile();
            });

            contextMenu.getItems().addAll(summarizeItem, new SeparatorMenuItem(), deleteItem);


            // Chỉ hiện menu khi dòng không rỗng
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );
            return row;
        });
    }

    private void onSummarizeFile(SharedFileLocal fileMeta) {
        File readFile = new File(shareFolderField.getText(), fileMeta.getFileName());

        if (!readFile.exists()) {
            showSuccessDialog("Lỗi", "File không tồn tại trên ổ cứng.");
            return;
        }

        // Hiện thông báo chờ
        if (globalStatusLabel != null) globalStatusLabel.setText("🤖 AI đang đọc và tóm tắt file...");

        // chay ngam
        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                return documentSummaryService.summarize(readFile);
            }
        };

        task.setOnSucceeded(e -> {
            if (globalStatusLabel != null) globalStatusLabel.setText("🤖 AI đã hoàn thành tóm tắt file.");
            String summary = task.getValue();

            showSummaryResultDialog(fileMeta.getFileName(), summary);
        });

        task.setOnFailed(e -> {
            if (globalStatusLabel != null) globalStatusLabel.setText("🤖 AI không thể tóm tắt file.");
            showSuccessDialog("Lỗi", "AI không thể tóm tắt file do lỗi xảy ra.");
            e.getSource().getException().printStackTrace();
        });
        new Thread(task).start();
    }

    // Load thư mục chia sẻ đã lưu trong AppConfig
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

    // Chọn thư mục chia sẻ
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
        File dir = chooser.showDialog(stage);   // show dialog

        if (dir != null) {
            shareFolderField.setText(dir.getAbsolutePath());
            AppConfig.save(KEY_SHARE_DIR, dir.getAbsolutePath()); // lưu cấu hình
            fileShareService.setShareFolder(dir);  // áp dụng thư mục chia sẻ

            refreshSharedFiles();

            if (globalStatusLabel != null) {
                globalStatusLabel.setText("Thư mục chia sẻ: " + dir.getName());
            }
        }


    }

    // Refresh lại bảng file
    @FXML
    private void onRefreshSharedFiles() {
        refreshSharedFiles();

        if (globalStatusLabel != null) {
            globalStatusLabel.setText("Đã quét lại thư mục và cập nhật Tag.");
        }
    }

    private void refreshSharedFiles() {
        List<SharedFileLocal> list = fileShareService.listSharedFiles(); // lấy danh sách file chia sẻ từ ổ cứng
        sharedFiles.setAll(list); // cập nhật lên bảng
        sharedFileTable.refresh(); // làm mới bảng
    }

    // Chưa implement add
    @FXML
    private void onAddSharedFile() {
        showSuccessDialog("Hướng dẫn", "Để thêm file, bạn chỉ cần copy file vào thư mục:\n" + shareFolderField.getText() + "\nSau đó bấm 'Quét lại'.");
    }

    @FXML
    private void onRemoveSharedFile() {
        SharedFileLocal selected = sharedFileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showSuccessDialog("Thông báo", "Vui lòng chọn file để xóa.");
            return;
        }

        boolean confirmed = showConfirmDialog(
                "🗑 Xác nhận xóa",
                "Xóa file: " + selected.getFileName() + "?",
                "Hành động này sẽ xóa file khỏi ổ cứng vĩnh viễn."
        );

        if (confirmed) {
            // Lấy đường dẫn file thật
            File fileToDelete = new File(shareFolderField.getText(), selected.getFileName());
            // Thực hiện xóa
            if (fileToDelete.exists() && fileToDelete.delete()) {
                // Xóa thành công -> Cập nhật lại giao diện
                sharedFiles.remove(selected); // Xóa khỏi bảng
                notifyPeersFileRemoved(selected.getFileName());
                refreshSharedFiles();
                showSuccessDialog("Thành công", "Đã xóa file thành công!");
            } else {
                showConfirmDialog("Lỗi", "Không thể xóa file", "File đang được mở hoặc bạn không có quyền xóa.");
            }
        }
    }

    private void notifyPeersFileRemoved(String fileName) {
        List<PeerInfo> activePeers = peerTabController.getActiveConnectedPeers();
        String command = "CMD:REMOVE_FILE|" + fileName;
        for (PeerInfo p : activePeers) {
            controlClient.sendSystemCommand(p, "REMOVE_FILE|" + fileName);
            System.out.println("Đã báo cho " + p.getName() + " xóa file: " + fileName);
        }
    }

    private boolean showConfirmDialog(String title, String header, String content) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/p2pfileshare/ConfirmationDialog.fxml"));
            Parent page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            if (shareFolderField.getScene() != null) {
                dialogStage.initOwner(shareFolderField.getScene().getWindow());
            }

            dialogStage.setScene(new Scene(page));

            ConfirmationController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setContent(title, header, content, "Đồng ý"); // Nút mặc định hoặc set text tùy ý

            if (title.contains("xóa") || title.contains("Lỗi")) {
                controller.setStyleDanger();
            }

            dialogStage.showAndWait();
            return controller.isConfirmed();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showSuccessDialog(String header, String content) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/p2pfileshare/ConfirmationDialog.fxml"));
            Parent page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            if (sharedFileTable.getScene() != null) {
                dialogStage.initOwner(sharedFileTable.getScene().getWindow());
            }

            dialogStage.setScene(new Scene(page));

            ConfirmationController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            // Set nội dung
            controller.setContent("Thông báo", header, content, "Đóng");

            // GỌI HÀM MỚI ĐỂ CHUYỂN GIAO DIỆN SANG MÀU XANH & ẨN NÚT HỦY
            controller.setStyleSuccess();

            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm này dùng Alert chuẩn của JavaFX để có TextArea (Cho phép cuộn và copy text)
    private void showSummaryResultDialog(String fileName, String summaryContent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Kết quả Tóm tắt AI");
        alert.setHeaderText("✨ Tóm tắt nội dung file: " + fileName);

        // Tạo TextArea để chứa nội dung dài
        TextArea textArea = new TextArea(summaryContent);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        // Chỉnh kích thước khung text
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 1);

        // Set vào Alert
        alert.getDialogPane().setContent(expContent);
        alert.showAndWait();
    }

    // hàm format size của file
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
