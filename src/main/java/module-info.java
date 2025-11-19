module org.example.p2pfileshare {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.google.gson;

    opens org.example.p2pfileshare to javafx.fxml;
    exports org.example.p2pfileshare;
    opens org.example.p2pfileshare.model to javafx.base;
    exports org.example.p2pfileshare.controller;
    opens org.example.p2pfileshare.controller to javafx.fxml;

//    opens org.example.p2pfileshare.protocol to com.google.gson;


}