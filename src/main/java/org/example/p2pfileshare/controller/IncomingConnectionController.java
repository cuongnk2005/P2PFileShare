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

public class IncomingConnectionController {

    @FXML
    private TextField nameField;
    private Stage stage;
    private String result = null;

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

}