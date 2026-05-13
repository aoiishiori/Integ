package com.team2.wordy.client.player.controller;

import com.team2.wordy.client.player.view.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.shape.Circle;

public class GameScreen {

    @FXML
    private TilePane letterContainer;

    @FXML
    private Circle mainPlayerAvatar;

    @FXML
    private Label mainUsername;

    @FXML
    private Label mainWinCount;

    @FXML
    private Circle opponentAvatar;

    @FXML
    private Label opponentUsername;

    @FXML
    private Label opponentWinCount;

    @FXML
    private Label roundLabel;

    @FXML
    private Label timerLabel;
}
