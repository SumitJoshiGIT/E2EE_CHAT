module com.e2ee.chat.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.controlsfx.controls;
    requires static lombok;
    requires java.sql;
    requires java.logging;
    requires java.desktop;
    requires spring.messaging;
    requires spring.websocket;
    requires spring.web;
    requires spring.core;
    requires spring.context;
    requires transitive javafx.graphics;

    opens com.e2ee.chat.frontend to javafx.fxml;
    opens com.e2ee.chat.frontend.controller to javafx.fxml;
    opens com.e2ee.chat.frontend.model to javafx.fxml, com.fasterxml.jackson.databind;
    opens com.e2ee.chat.frontend.service to javafx.fxml, com.fasterxml.jackson.databind;
    opens com.e2ee.chat.frontend.crypto to javafx.fxml;
    
    exports com.e2ee.chat.frontend;
    exports com.e2ee.chat.frontend.controller;
    exports com.e2ee.chat.frontend.model;
    exports com.e2ee.chat.frontend.service;
    exports com.e2ee.chat.frontend.crypto;
}
