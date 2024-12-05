package com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity;


import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.DelReStockItemPK;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_return_item_status")
@IdClass(DelReStockItemPK.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelReStockItem {

    @Id
    @Column(name = "return_stock_history_code")
    private Integer returnStockHistoryCode;

    @Id
    @Column(name = "item_code")
    private Integer itemCode;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "restock_quantity", nullable = false)
    private int restockQuantity;

    // Getters and Setters
}