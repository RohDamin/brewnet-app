package com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_exchange_stock_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelExStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_stock_history_code")
    private int exchangeStockHistoryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExchangeStatus status;

    @Column(name = "manager", nullable = false, length = 255)
    private String manager;

    @Column(name = "comment", length = 255)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmed", nullable = false)
    private ConfirmationStatus confirmed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "exchange_code", nullable = false)
    private Integer exchangeCode;

    // Getters and Setters

    public enum ExchangeStatus {
        TOTAL_INBOUND, TOTAL_DISPOSAL, PARTIAL_INBOUND
    }

    public enum ConfirmationStatus {
        CONFIRMED, UNCONFIRMED
    }
}
