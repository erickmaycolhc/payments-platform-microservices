package com.service.transaction.entity;

import com.service.transaction.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transfer",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_transfer_idempotency_key",
                columnNames = "idempotency_key"
        ))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_transfer", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "source_account", nullable = false, length = 50)
    private String sourceAccount; //origen

    @Column(name = "target_account", nullable = false, length = 50)
    private String targetAccount; //destino

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransferStatus status;

    @Column(name = "failure_reason")
    private String failureReason; // motivo fallo

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
