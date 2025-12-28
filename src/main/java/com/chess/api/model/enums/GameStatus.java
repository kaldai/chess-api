package com.chess.api.model.enums;

import lombok.Getter;

@Getter
public enum GameStatus {
  WAITING("Ожидание игрока"),
  ACTIVE("Игра идет"),
  PAUSED("Игра приостановлена"),
  DRAW_PROPOSED("Предложена ничья"),
  ABORTED("Игра прервана"),
  WHITE_WON("Белые победили"),
  BLACK_WON("Черные победили"),
  DRAW("Ничья");

  private final String description;

  GameStatus(String description) {
    this.description = description;
  }

}