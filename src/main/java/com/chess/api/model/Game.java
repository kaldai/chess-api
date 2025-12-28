package com.chess.api.model;

import com.chess.api.model.enums.GameResult;
import com.chess.api.model.enums.GameStatus;
import com.chess.api.model.enums.GameType;
import com.github.bhlangonijr.chesslib.Board;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "white_player_id", nullable = false)
  private Player whitePlayer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "black_player_id")
  private Player blackPlayer;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GameType gameType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GameStatus status = GameStatus.WAITING;

  @Enumerated(EnumType.STRING)
  private GameResult result;

  @Column(columnDefinition = "TEXT")
  private String initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  @Column(columnDefinition = "TEXT")
  private String currentFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  @Column(columnDefinition = "TEXT")
  private String pgn;

  private Integer timeControl = 600; // секунды
  private Integer timeIncrement = 0; // секунды

  private Integer whiteTimeLeft; // миллисекунды
  private Integer blackTimeLeft; // миллисекунды

  private LocalDateTime createdAt = LocalDateTime.now();
  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;

  @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @OrderBy("moveNumber ASC")
  private List<Move> moves = new ArrayList<>();

  @Transient
  private transient com.github.bhlangonijr.chesslib.Board chessBoard;

  public void initializeChessBoard() {
    chessBoard = new Board();
    chessBoard.loadFromFen(initialFen);
  }

  public Board getChessBoard() {
    if (chessBoard == null) {
      initializeChessBoard();
    }
    return chessBoard;
  }

  public boolean isPlayerInGame(Player player) {
    return player != null && (player.equals(whitePlayer) || player.equals(blackPlayer));
  }

  public boolean isPlayerTurn(Player player) {
    if (!isPlayerInGame(player)) return false;

    Board board = getChessBoard();
    if (board == null) return false;

    com.github.bhlangonijr.chesslib.Side sideToMove = board.getSideToMove();
    if (sideToMove == com.github.bhlangonijr.chesslib.Side.WHITE) {
      return player.equals(whitePlayer);
    } else {
      return player.equals(blackPlayer);
    }
  }
}