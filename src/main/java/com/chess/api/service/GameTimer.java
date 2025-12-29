package com.chess.api.service;

public interface GameTimer {

  void switchTurn();

  int getWhiteTimeLeft();

  int getBlackTimeLeft();

  void stop();

}