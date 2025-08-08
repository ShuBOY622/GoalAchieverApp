package org.goalapp.challenge.repository;

import org.goalapp.challenge.entities.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    List<Challenge> findByChallengedUserIdAndStatus(Long challengedUserId, Challenge.ChallengeStatus status);

    List<Challenge> findByChallengerIdOrderByCreatedAtDesc(Long challengerId);

    List<Challenge> findByChallengerId(Long challengerId);

    List<Challenge> findByChallengedUserId(Long challengedUserId);
}
