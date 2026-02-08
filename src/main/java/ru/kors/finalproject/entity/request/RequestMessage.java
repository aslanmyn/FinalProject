package ru.kors.finalproject.entity.request;

import jakarta.persistence.*;
import lombok.*;
import ru.kors.finalproject.entity.user.User;

import java.time.Instant;

@Entity
@Table(name = "request_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private RequestTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT")
    private String body;
    private Instant sentAt;
}
