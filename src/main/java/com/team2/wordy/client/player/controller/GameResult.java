package com.team2.wordy.client.player.controller;

import com.team2.wordy.client.player.view.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class GameResult {

    @FXML
    private Button homeButton;

    @FXML
    private VBox playerListContainer;

    @FXML
    private Circle winnerAvatar;

    @FXML
    private Label winnerLongestWord;

    @FXML
    private Label winnerUsername;

}
