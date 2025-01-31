package com.varc.brewnetapp.domain.delivery.command.domain.aggregate;


import jakarta.persistence.Embeddable;
import java.io.Serializable;
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
public class DelRefundItemPK implements Serializable {

    private int returnRefundHistoryCode;
    private int itemCode;
}