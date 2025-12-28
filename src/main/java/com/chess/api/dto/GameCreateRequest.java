package com.chess.api.dto;

import com.chess.api.model.enums.GameType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameCreateRequest {

  @NotNull(message = "Тип игры обязателен")
  private GameType gameType;

  private Integer timeControl; // если null, используется значение по умолчанию для типа
  private Integer timeIncrement;

  private Boolean isPublic = true;
  private Long opponentId; // если null - игра ожидает случайного соперника

}