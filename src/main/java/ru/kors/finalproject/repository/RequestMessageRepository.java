package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.RequestMessage;

import java.util.List;

public interface RequestMessageRepository extends JpaRepository<RequestMessage, Long> {
    List<RequestMessage> findByRequestIdOrderByCreatedAtAsc(Long requestId);

    @Query("SELECT m FROM RequestMessage m LEFT JOIN FETCH m.sender WHERE m.request.id = :requestId ORDER BY m.createdAt ASC")
    List<RequestMessage> findByRequestIdWithSenderOrderByCreatedAtAsc(Long requestId);
}
