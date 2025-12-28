package com.chess.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveDTO {
  private Long id;
  private Integer moveNumber;
  private String fromSquare;
  private String toSquare;
  private String promotion;
  private String san;
  private Integer whiteTimeLeft;
  private Integer blackTimeLeft;
  private LocalDateTime timestamp;
}