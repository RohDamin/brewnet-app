package com.varc.brewnetapp.domain.order.command.domain.aggregate.entity;

import com.varc.brewnetapp.common.domain.order.Available;
import com.varc.brewnetapp.domain.order.command.domain.aggregate.entity.compositionkey.OrderItemCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity(name = "tbl_order_item")
public class OrderItem {
    @EmbeddedId
    private OrderItemCode orderItemCode;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "available", nullable = false)
    private Available available;

    @Column(name = "part_sum_price", nullable = false)
    private int partSumPrice;
}
