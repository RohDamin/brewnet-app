package com.varc.brewnetapp.domain.returning.query.aggregate.vo;

import com.varc.brewnetapp.common.domain.approve.Confirmed;
import com.varc.brewnetapp.common.domain.returning.ReturningHistoryStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ReturningHistoryVO {
    private int returningStockHistoryCode;
    private ReturningHistoryStatus status;
    private String manager;
    private String createdAt;
    private Confirmed confirmed;
    private String returningCode;
    private String returningManager;
    private String returningCreatedAt;
}

