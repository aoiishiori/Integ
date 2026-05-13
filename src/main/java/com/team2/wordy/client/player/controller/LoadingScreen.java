package com.team2.wordy.client.player.controller;

import com.team2.wordy.client.player.view.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

public class LoadingScreen {

    @FXML
    private Circle player1Avatar;

    @FXML
    private Label player1Name;

    @FXML
    private Label player1Status;

    @FXML
    private Circle player2Avatar;

    @FXML
    private Label player2Name;

    @FXML
    private Label player2Status;

    @FXML
    private Text statusText;

    @FXML
    private Text subStatusText;

    @FXML
    private Label timerLabel;

}
