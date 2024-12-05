package com.varc.brewnetapp.domain.returning.query.aggregate.vo;

import com.varc.brewnetapp.common.domain.approve.Confirmed;
import com.varc.brewnetapp.common.domain.returning.ReturningRefundStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class RefundHistoryVO {
    private int returningRefundHistoryCode;
    private ReturningRefundStatus status;
    private String manager;
    private String createdAt;
    private Confirmed confirmed;
    private String returningCode;
    private String returningManager;
    private String returningCreatedAt;
}
