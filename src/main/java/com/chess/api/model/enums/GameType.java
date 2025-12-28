package com.chess.api.model.enums;

import lombok.Getter;

@Getter
public enum GameType {
  CLASSICAL("Классические шахматы", 1800, 30),
  RAPID("Рапид", 600, 5),
  BLITZ("Блиц", 180, 2);

  private final String description;
  private final int baseTime; // секунды
  private final int increment; // секунды

  GameType(String description, int baseTime, int increment) {
    this.description = description;
    this.baseTime = baseTime;
    this.increment = increment;
  }

}