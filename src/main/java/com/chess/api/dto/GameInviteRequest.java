package com.chess.api.dto;

import com.chess.api.model.enums.GameType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameInviteRequest {

  @NotNull(message = "ID игрока обязателен")
  private Long playerId;

  @NotNull(message = "Тип игры обязателен")
  private GameType gameType;

  private Integer timeControl;
  private Integer timeIncrement;
}