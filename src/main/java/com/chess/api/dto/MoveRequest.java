package com.chess.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveRequest {

  @NotBlank(message = "Поле from обязательно")
  @Pattern(regexp = "^[a-h][1-8]$", message = "Неверный формат начальной клетки")
  private String from;

  @NotBlank(message = "Поле to обязательно")
  @Pattern(regexp = "^[a-h][1-8]$", message = "Неверный формат конечной клетки")
  private String to;

  @Pattern(regexp = "^[QRBN]?$", message = "Неверный символ превращения. Используйте Q, R, B или N")
  private String promotion;

  private Long timestamp; // время отправки хода (для проверки таймера)
}