package com.varc.brewnetapp.domain.delivery.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
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
public class DelReStockItemPK {

    private Integer returnStockHistoryCode;
    private Integer itemCode;
}
