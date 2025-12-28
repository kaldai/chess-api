package com.chess.api.dto;

import com.chess.api.model.enums.GameType;
import com.chess.api.model.enums.InviteStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameInviteDTO {
  private Long id;
  private PlayerDTO sender;
  private PlayerDTO receiver;
  private GameType gameType;
  private InviteStatus status;
  private Integer timeControl;
  private Integer timeIncrement;
  private LocalDateTime sentAt;
  private LocalDateTime respondedAt;
  private Long gameId;
}