package com.service.notification.entity;

import com.service.notification.enums.NotificationType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("notification")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @Column("id_notification")
    private Long id;

    @Column("event_id")
    private String eventId;

    @Column("account_number")
    private String accountNumber;

    @Column("transfer_id")
    private UUID transferId;

    @Column("type")
    private NotificationType type;

    @Column("message")
    private String message;

    @Column("created_at")
    private LocalDateTime createdAt;
}
