package com.chess.api.service;

import com.github.bhlangonijr.chesslib.Side;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Slf4j
@Getter
public class GameTimer implements Runnable {

  private final Long gameId;
  private final AtomicInteger whiteTimeLeft;
  private final AtomicInteger blackTimeLeft;
  private final int increment; // в миллисекундах
  private final BiConsumer<Long, Side> timeExpiredCallback;

  private Side currentSide = Side.WHITE;
  private final AtomicBoolean running = new AtomicBoolean(true);
  private long lastUpdateTime;

  public GameTimer(Long gameId, int whiteTimeLeft, int blackTimeLeft,
                   int increment, BiConsumer<Long, Side> timeExpiredCallback) {
    this.gameId = gameId;
    this.whiteTimeLeft = new AtomicInteger(whiteTimeLeft);
    this.blackTimeLeft = new AtomicInteger(blackTimeLeft);
    this.increment = increment;
    this.timeExpiredCallback = timeExpiredCallback;
    this.lastUpdateTime = System.currentTimeMillis();
  }

  @Override
  public void run() {
    if (!running.get()) {
      return;
    }

    long currentTime = System.currentTimeMillis();
    long elapsed = currentTime - lastUpdateTime;

    if (currentSide == Side.WHITE) {
      int newTime = whiteTimeLeft.get() - (int) elapsed;
      if (newTime <= 0) {
        whiteTimeLeft.set(0);
        running.set(false);
        timeExpiredCallback.accept(gameId, Side.WHITE);
        return;
      }
      whiteTimeLeft.set(newTime);
    } else {
      int newTime = blackTimeLeft.get() - (int) elapsed;
      if (newTime <= 0) {
        blackTimeLeft.set(0);
        running.set(false);
        timeExpiredCallback.accept(gameId, Side.BLACK);
        return;
      }
      blackTimeLeft.set(newTime);
    }

    lastUpdateTime = currentTime;
  }

  public void switchTurn() {
    if (currentSide == Side.WHITE) {
      whiteTimeLeft.addAndGet(increment);
    } else {
      blackTimeLeft.addAndGet(increment);
    }

    currentSide = currentSide.flip();
    lastUpdateTime = System.currentTimeMillis();
  }

  public int getWhiteTimeLeft() {
    return whiteTimeLeft.get();
  }

  public int getBlackTimeLeft() {
    return blackTimeLeft.get();
  }

  public void stop() {
    running.set(false);
  }
}