package org.example.p2pfileshare.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

import org.example.p2pfileshare.network.control.ControlClient;
import org.example.p2pfileshare.network.control.ControlProtocol;
import org.example.p2pfileshare.network.control.ControlServer;
import org.example.p2pfileshare.service.PeerService;
import org.example.p2pfileshare.util.AppConfig;

public class ChangeNameController {
    @FXML private TextField nameField;
    private Stage stage;
    private String result = null;
    private ControlClient controlClient;
    private ControlServer ControlServer;
    private Consumer<String> onUpdatePeerName;
    public static final String KEY_PEER_NAME = "peer.displayName";
    @FXML
    private void initialize() {
        // no-op
    }

    @FXML
    private void onSave() {
        String v = nameField.getText();
        if (v != null) v = v.trim();
        if (v == null || v.isEmpty()) {
            return; // don't close
        }
        result = v;

        // Lưu vào AppConfig để lần sau mở app có thể lấy lại tên này
        AppConfig.save(KEY_PEER_NAME, result);
        if (onUpdatePeerName != null) {
            try {
                onUpdatePeerName.accept(result);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        controlClient.broadcastUpdateName(ControlServer.getConnectedPeers(), result);
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

    public static Optional<String> showDialog(
            Window owner,
            String initialName,
            Consumer<String> onUpdatePeerName
    ) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ChangeNameController.class.getResource(
                            "/org/example/p2pfileshare/ChangeNameDialog.fxml"
                    )
            );
            Scene scene = new Scene(loader.load());
            ChangeNameController controller = loader.getController();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Đổi tên Peer");
            stage.setResizable(false);
            stage.setScene(scene);

            controller.stage = stage;

            // ✅ SET CALLBACK TỪ ROOT
            controller.setonUpdatePeerName(onUpdatePeerName);

            // load initial name
            String nameToShow = initialName;
            if (nameToShow == null || nameToShow.isEmpty()) {
                String saved = AppConfig.load(KEY_PEER_NAME);
                if (saved != null && !saved.isEmpty()) nameToShow = saved;
            }
            controller.setInitialName(nameToShow);

            stage.showAndWait();
            return Optional.ofNullable(controller.result);

        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public void setonUpdatePeerName(Consumer<String> callback) {
        this.onUpdatePeerName = callback;
    }
}
