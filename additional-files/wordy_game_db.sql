-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1:3306
-- Generation Time: Apr 24, 2026 at 03:42 AM
-- Server version: 9.1.0
-- PHP Version: 8.3.28

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `wordy_game_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `games`
--

DROP TABLE IF EXISTS `games`;
CREATE TABLE IF NOT EXISTS `games` (
  `game_id` int NOT NULL AUTO_INCREMENT,
  `session_id` varchar(50) NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING, IN PROGRESS, COMPLETED',
  `overall_winner` int DEFAULT NULL COMMENT 'user_id of the winner',
  PRIMARY KEY (`game_id`),
  UNIQUE KEY `session_id` (`session_id`),
  KEY `fk_game_winner` (`overall_winner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `game_config`
--

DROP TABLE IF EXISTS `game_config`;
CREATE TABLE IF NOT EXISTS `game_config` (
  `con_id` int NOT NULL DEFAULT '1',
  `join_wait_seconds` int NOT NULL DEFAULT '10',
  `round_duration_seconds` int NOT NULL DEFAULT '30',
  PRIMARY KEY (`con_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `game_config`
--

INSERT INTO `game_config` (`con_id`, `join_wait_seconds`, `round_duration_seconds`) VALUES
(1, 10, 30);

-- --------------------------------------------------------

--
-- Table structure for table `game_participants`
--

DROP TABLE IF EXISTS `game_participants`;
CREATE TABLE IF NOT EXISTS `game_participants` (
  `part_id` int NOT NULL AUTO_INCREMENT,
  `game_id` int NOT NULL,
  `player_id` int NOT NULL,
  `round_wins` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`part_id`),
  UNIQUE KEY `game_id` (`game_id`,`player_id`),
  KEY `fk_participant_player` (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `longest_words_record`
--

DROP TABLE IF EXISTS `longest_words_record`;
CREATE TABLE IF NOT EXISTS `longest_words_record` (
  `rec_id` int NOT NULL AUTO_INCREMENT,
  `word` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `word_length` int NOT NULL,
  `submitted_by` int NOT NULL,
  PRIMARY KEY (`rec_id`),
  KEY `fk_longest_player` (`submitted_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `player_stats`
--

DROP TABLE IF EXISTS `player_stats`;
CREATE TABLE IF NOT EXISTS `player_stats` (
  `stat_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `total_wins` int NOT NULL DEFAULT '0',
  `longest_word` varchar(50) DEFAULT NULL,
  `longest_word_length` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`stat_id`),
  UNIQUE KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `rounds`
--

DROP TABLE IF EXISTS `rounds`;
CREATE TABLE IF NOT EXISTS `rounds` (
  `round_id` int NOT NULL AUTO_INCREMENT,
  `game_id` int NOT NULL,
  `round_number` int NOT NULL,
  `letters` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `round_winner` int DEFAULT NULL,
  `winning_word` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`round_id`),
  KEY `fk_round_game` (`game_id`),
  KEY `fk_round_winner` (`round_winner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users` (Updated with is_archived)
--

DROP TABLE IF EXISTS `users`;
CREATE TABLE IF NOT EXISTS `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `role` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PLAYER',
  `is_archived` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0 = Active, 1 = Archived',
  `session_token` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'NULL = offline',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `username`, `password`, `role`, `is_archived`, `session_token`) VALUES
(1, 'admin1', '12345', 'ADMIN', 0, NULL),
(2, 'admin2', '54321', 'ADMIN', 0, NULL),
(3, 'player1', 'pass1', 'PLAYER', 0, NULL),
(4, 'player2', 'pass2', 'PLAYER', 0, NULL),
(5, 'player3', 'pass3', 'PLAYER', 0, NULL),
(6, 'player4', 'pass4', 'PLAYER', 0, NULL),
(7, 'player5', 'pass5', 'PLAYER', 0, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `word_submissions`
--

DROP TABLE IF EXISTS `word_submissions`;
CREATE TABLE IF NOT EXISTS `word_submissions` (
  `sub_id` int NOT NULL AUTO_INCREMENT,
  `round_id` int NOT NULL,
  `player_id` int NOT NULL,
  `word` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `is_valid` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`sub_id`),
  KEY `fk_submission_round` (`round_id`),
  KEY `fk_submission_player` (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `games`
--
ALTER TABLE `games`
  ADD CONSTRAINT `fk_game_winner` FOREIGN KEY (`overall_winner`) REFERENCES `users` (`user_id`) ON DELETE SET NULL;

--
-- Constraints for table `game_participants`
--
ALTER TABLE `game_participants`
  ADD CONSTRAINT `fk_participant_game` FOREIGN KEY (`game_id`) REFERENCES `games` (`game_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_participant_player` FOREIGN KEY (`player_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `longest_words_record`
--
ALTER TABLE `longest_words_record`
  ADD CONSTRAINT `fk_longest_player` FOREIGN KEY (`submitted_by`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `player_stats`
--
ALTER TABLE `player_stats`
  ADD CONSTRAINT `fk_stats_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `rounds`
--
ALTER TABLE `rounds`
  ADD CONSTRAINT `fk_round_game` FOREIGN KEY (`game_id`) REFERENCES `games` (`game_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_round_winner` FOREIGN KEY (`round_winner`) REFERENCES `users` (`user_id`) ON DELETE SET NULL;

--
-- Constraints for table `word_submissions`
--
ALTER TABLE `word_submissions`
  ADD CONSTRAINT `fk_submission_player` FOREIGN KEY (`player_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_submission_round` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
