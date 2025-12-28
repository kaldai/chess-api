package com.chess.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChessApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(ChessApiApplication.class, args);
  }
}