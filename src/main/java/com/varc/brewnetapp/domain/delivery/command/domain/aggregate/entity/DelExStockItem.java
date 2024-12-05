package com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity;

import com.varc.brewnetapp.domain.correspondent.command.domain.aggregate.CorrespondentItemId;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.DelExStockItemPK;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_exchange_item_status")
@IdClass(DelExStockItemPK.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelExStockItem {

    @Id
    @Column(name = "exchange_stock_history_code")
    private Integer exchangeStockHistoryCode;

    @Id
    @Column(name = "item_code")
    private Integer itemCode;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "restock_quantity", nullable = false)
    private int restockQuantity;

    // Getters and Setters
}