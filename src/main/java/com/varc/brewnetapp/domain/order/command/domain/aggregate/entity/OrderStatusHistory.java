package com.varc.brewnetapp.domain.order.command.domain.aggregate.entity;

import com.varc.brewnetapp.common.domain.order.OrderHistoryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity(name = "tbl_order_status_history")
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_status_history_code", nullable = false)
    private Integer orderStatusHistoryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderHistoryStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "active", nullable = false)
    private final boolean active = true;

    @Column(name = "order_code", nullable = false)
    private int orderCode;
}
