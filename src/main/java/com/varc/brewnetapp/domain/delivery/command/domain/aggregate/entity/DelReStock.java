package com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity;


import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_return_stock_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelReStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_stock_history_code")
    private int returnStockHistoryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StockStatus status;

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

    @Column(name = "return_code", nullable = false)
    private Integer returnCode;

    // Getters and Setters

    public enum StockStatus {
        TOTAL_INBOUND, PARTIAL_INBOUND, TOTAL_DISPOSAL
    }

    public enum ConfirmationStatus {
        CONFIRMED, UNCONFIRMED
    }
}