package com.chess.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
  private String sender;
  private String content;
  private LocalDateTime timestamp;
  private String type; // MESSAGE, SYSTEM, etc.
}