package com.chess.api.model;

import com.chess.api.model.enums.GameType;
import com.chess.api.model.enums.InviteStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "invites")
@Data
@NoArgsConstructor
public class GameInvite {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private Player sender;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receiver_id", nullable = false)
  private Player receiver;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GameType gameType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InviteStatus status = InviteStatus.PENDING;

  private Integer timeControl;
  private Integer timeIncrement;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "game_id")
  private Game game;

  private LocalDateTime sentAt = LocalDateTime.now();
  private LocalDateTime respondedAt;
  private LocalDateTime expiresAt;

  @PrePersist
  protected void onCreate() {
    if (sentAt == null) {
      sentAt = LocalDateTime.now();
    }
    if (expiresAt == null) {
      expiresAt = sentAt.plusDays(7); // Приглашение истекает через 7 дней
    }
  }
}