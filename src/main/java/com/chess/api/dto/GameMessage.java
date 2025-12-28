package com.chess.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameMessage {
  private String type; // MOVE_RECEIVED, GAME_STARTED, GAME_ENDED, etc.
  private Object content;
  private String sender;
  private LocalDateTime timestamp;
}