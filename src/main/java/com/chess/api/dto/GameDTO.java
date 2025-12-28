package com.chess.api.dto;

import com.chess.api.model.enums.GameStatus;
import com.chess.api.model.enums.GameType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameDTO {
  private Long id;
  private PlayerDTO whitePlayer;
  private PlayerDTO blackPlayer;
  private GameType gameType;
  private GameStatus status;
  private String currentFen;
  private String pgn;
  private Integer timeControl;
  private Integer timeIncrement;
  private Integer whiteTimeLeft;
  private Integer blackTimeLeft;
  private LocalDateTime createdAt;
  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;
  private List<MoveDTO> moves;
  private String result;

}