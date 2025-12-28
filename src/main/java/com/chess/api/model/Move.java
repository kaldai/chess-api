package com.chess.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "moves")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Move {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "game_id", nullable = false)
  private Game game;

  private Integer moveNumber;

  @Column(length = 10)
  private String fromSquare;

  @Column(length = 10)
  private String toSquare;

  @Column(length = 1)
  private String promotion; // Q, R, B, N

  @Column(length = 10)
  private String san; // Стандартная алгебраическая нотация

  private Integer whiteTimeLeft; // остаток времени после хода
  private Integer blackTimeLeft;

  private LocalDateTime timestamp = LocalDateTime.now();
}