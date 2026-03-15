package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.ChatRoom;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findBySectionIdAndType(Long sectionId, ChatRoom.ChatRoomType type);

    @Query("""
        SELECT cr FROM ChatRoom cr
        WHERE cr.id IN (SELECT m.chatRoom.id FROM ChatRoomMember m WHERE m.user.id = :userId)
        ORDER BY cr.createdAt DESC
    """)
    List<ChatRoom> findRoomsByUserId(Long userId);

    @Query("""
        SELECT cr FROM ChatRoom cr
        WHERE cr.type = 'DIRECT'
          AND cr.id IN (SELECT m1.chatRoom.id FROM ChatRoomMember m1 WHERE m1.user.id = :userId1)
          AND cr.id IN (SELECT m2.chatRoom.id FROM ChatRoomMember m2 WHERE m2.user.id = :userId2)
    """)
    Optional<ChatRoom> findDirectRoomBetween(Long userId1, Long userId2);
}
