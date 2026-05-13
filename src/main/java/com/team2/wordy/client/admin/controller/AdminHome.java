package com.team2.wordy.client.admin.controller;

import com.team2.wordy.client.admin.view.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Circle;

public class AdminHome {

    @FXML
    private Circle adminAvatarCircle;

    @FXML
    private Label adminNameLbl;

    @FXML
    private Button dashboardBtn;

    @FXML
    private Button logoutBtn;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Label offlineCountLbl;

    @FXML
    private Label onlineCountLbl;

    @FXML
    private Button playerFilterBtn;

    @FXML
    private Button playerSearchBtn;

    @FXML
    private TextField playerSearchTxt;

    @FXML
    private AnchorPane playerTableContainer;

    @FXML
    private Tab requestsTab;

    @FXML
    private Button settingsBtn;
}
