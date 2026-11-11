package com.service.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_message")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProcessedMessageEntity {

    @Id
    @Column(name = "message_id")
    private String messageId; // id message kafka

    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;
}
