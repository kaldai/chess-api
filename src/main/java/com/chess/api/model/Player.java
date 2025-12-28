package com.chess.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Имя пользователя обязательно")
  @Size(min = 3, max = 50, message = "Имя пользователя должно быть от 3 до 50 символов")
  @Column(unique = true, nullable = false)
  private String username;

  @NotBlank(message = "Email обязателен")
  @Email(message = "Неверный формат email")
  @Column(unique = true, nullable = false)
  private String email;

  @JsonIgnore
  @NotBlank(message = "Пароль обязателен")
  @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
  private String password;

  private Integer classicalRating = 1200;
  private Integer rapidRating = 1200;
  private Integer blitzRating = 1200;

  private Integer gamesPlayed = 0;
  private Integer gamesWon = 0;
  private Integer gamesDrawn = 0;
  private Integer gamesLost = 0;

  private LocalDateTime createdAt = LocalDateTime.now();
  private LocalDateTime lastLoginAt;

  @JsonIgnore
  @OneToMany(mappedBy = "whitePlayer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Game> gamesAsWhite = new ArrayList<>();

  @JsonIgnore
  @OneToMany(mappedBy = "blackPlayer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Game> gamesAsBlack = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}