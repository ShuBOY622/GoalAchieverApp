package org.goalapp.user.repository;

import org.goalapp.user.entities.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    List<FriendRequest> findByToUserIdAndStatus(Long toUserId, FriendRequest.Status status);
    List<FriendRequest> findByFromUserIdAndStatus(Long fromUserId, FriendRequest.Status status);

    @Query("SELECT fr FROM FriendRequest fr WHERE " +
            "(fr.fromUserId = ?1 AND fr.toUserId = ?2) OR " +
            "(fr.fromUserId = ?2 AND fr.toUserId = ?1)")
    Optional<FriendRequest> findExistingRequest(Long userId1, Long userId2);

    @Query("SELECT CASE WHEN fr.fromUserId = ?1 THEN fr.toUserId ELSE fr.fromUserId END " +
            "FROM FriendRequest fr WHERE " +
            "(fr.fromUserId = ?1 OR fr.toUserId = ?1) AND fr.status = 'ACCEPTED'")
    List<Long> findFriendsIds(Long userId);
}
