package com.varc.brewnetapp.domain.exchange.query.aggregate.vo;

import com.varc.brewnetapp.common.domain.approve.Confirmed;
import com.varc.brewnetapp.common.domain.exchange.ExchangeHistoryStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ExchangeHistoryVO {
    private int exchangeStockHistoryCode;
    private ExchangeHistoryStatus status;
    private String manager;
    private String createdAt;
    private Confirmed confirmed;
    private String exchangeCode;
    private String exchangeManager;
    private String exchangeCreatedAt;
}
