package com.service.transaction.entity;

import com.service.transaction.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_status_history")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransferStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transfer_status_history")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = false)
    private TransferEntity transfer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransferStatus status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "change_at", nullable = false, updatable = false)
    private LocalDateTime changeAt;
}
