package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.RequestMessage;

import java.util.List;

public interface RequestMessageRepository extends JpaRepository<RequestMessage, Long> {
    List<RequestMessage> findByRequestIdOrderByCreatedAtAsc(Long requestId);
}
