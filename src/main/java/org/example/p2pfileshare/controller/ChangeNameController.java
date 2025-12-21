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

public class ChangeNameController {

    @FXML private TextField nameField;
    private Stage stage;
    private String result = null;
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

    public static Optional<String> showDialog(Window owner, String initialName) {
        try {
            FXMLLoader loader = new FXMLLoader(ChangeNameController.class.getResource("/org/example/p2pfileshare/ChangeNameDialog.fxml"));
            Scene scene = new Scene(loader.load());
            ChangeNameController controller = loader.getController();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Đổi tên Peer");
            stage.setResizable(false);
            stage.setScene(scene);

            controller.stage = stage;
            controller.setInitialName(initialName);

            stage.showAndWait();

            return Optional.ofNullable(controller.result);

        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
