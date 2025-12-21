package org.example.p2pfileshare.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.example.p2pfileshare.network.control.ControlClient;
import org.example.p2pfileshare.network.control.ControlServer;
import org.example.p2pfileshare.util.AppConfig;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

public class ChangeNameController {

    @FXML private TextField nameField;

    private Stage stage;
    private String result;

    private ControlClient controlClient;
    private ControlServer controlServer;

    private Consumer<String> onUpdatePeerName;

    public static final String KEY_PEER_NAME = "peer.displayName";

    // ✅ BẮT BUỘC cho FXMLLoader
    public ChangeNameController() {}

    /**
     * Inject các dependency sau khi FXMLLoader load xong
     */
    public void init(Stage stage,
                     ControlClient controlClient,
                     ControlServer controlServer,
                     Consumer<String> callback) {
        this.stage = stage;
        this.controlClient = controlClient;
        this.controlServer = controlServer;
        this.onUpdatePeerName = callback;
    }

    @FXML
    private void onSave() {
        String v = nameField.getText();
        if (v == null) v = "";
        v = v.trim();

        if (v.isEmpty()) return;

        result = v;

        // lưu cấu hình
        AppConfig.save(KEY_PEER_NAME, v);

        // callback về Root
        if (onUpdatePeerName != null) {
            try {
                onUpdatePeerName.accept(v);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // broadcast cho các peer đang connected
        if (controlClient != null && controlServer != null) {
            try {
                controlClient.broadcastUpdateName(controlServer.getConnectedPeers(), v);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (stage != null) stage.close();
    }

    @FXML
    private void onCancel() {
        result = null;
        if (stage != null) stage.close();
    }

    public void setInitialName(String name) {
        nameField.setText(name != null ? name : "");
        nameField.requestFocus();
        nameField.selectAll();
    }

    /**
     * ✅ showDialog bản đúng: truyền controlClient + controlServer để init()
     */
    public static Optional<String> showDialog(
            Window owner,
            String initialName,
            ControlClient controlClient,
            ControlServer controlServer,
            Consumer<String> onUpdatePeerName
    ) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ChangeNameController.class.getResource("/org/example/p2pfileshare/ChangeNameDialog.fxml")
            );

            Scene scene = new Scene(loader.load());
            ChangeNameController controller = loader.getController();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Đổi tên Peer");
            stage.setResizable(false);
            stage.setScene(scene);

            // ✅ init dependency
            controller.init(stage, controlClient, controlServer, onUpdatePeerName);

            // load initial name (ưu tiên initialName, fallback config)
            String nameToShow = initialName;
            if (nameToShow == null || nameToShow.isBlank()) {
                String saved = AppConfig.load(KEY_PEER_NAME);
                if (saved != null && !saved.isBlank()) nameToShow = saved;
            }
            controller.setInitialName(nameToShow);

            stage.showAndWait();
            return Optional.ofNullable(controller.result);

        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
