package com.chess.api.exception;

public class IllegalMoveException extends Exception {
  public IllegalMoveException(String message) {
    super(message);
  }
}