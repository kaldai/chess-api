package com.chess.api.repository;

import com.chess.api.model.Move;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {

  List<Move> findByGameIdOrderByMoveNumberAsc(Long gameId);
}