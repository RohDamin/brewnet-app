package com.varc.brewnetapp.domain.delivery.command.domain.aggregate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class DelExStockItemPK {

    private int exchangeStockHistoryCode;
    private int itemCode;
}
