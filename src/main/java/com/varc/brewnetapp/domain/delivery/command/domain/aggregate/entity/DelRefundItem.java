package com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity;

import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.DelExStockItemPK;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_refund_item_status")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(DelRefundItem.class)
public class DelRefundItem  {
    @Id
    @Column(name = "return_refund_history_code")
    private Integer returnRefundHistoryCode;

    @Id
    @Column(name = "item_code")
    private Integer itemCode;


    @Column(name = "completed", nullable = false)
    private boolean completed;

    // Getters and Setters
}