package com.chess.api.repository;

import com.chess.api.model.GameInvite;
import com.chess.api.model.Player;
import com.chess.api.model.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameInviteRepository extends JpaRepository<GameInvite, Long> {

  List<GameInvite> findByReceiverAndStatus(Player receiver, InviteStatus status);

  List<GameInvite> findBySenderAndStatus(Player sender, InviteStatus status);

  Optional<GameInvite> findByIdAndReceiver(Long id, Player receiver);

  Optional<GameInvite> findByIdAndSender(Long id, Player sender);

  boolean existsBySenderAndReceiverAndStatus(Player sender, Player receiver, InviteStatus status);
}