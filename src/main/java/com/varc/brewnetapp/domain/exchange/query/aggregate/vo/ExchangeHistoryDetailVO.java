package com.varc.brewnetapp.domain.exchange.query.aggregate.vo;

import com.varc.brewnetapp.common.domain.approve.Confirmed;
import com.varc.brewnetapp.common.domain.exchange.ExchangeHistoryStatus;
import com.varc.brewnetapp.common.domain.exchange.ExchangeReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter // 품목 조회 위해 필요
public class ExchangeHistoryDetailVO {
    private int exchangeStockHistoryCode;
    private int exchangeCode;
    private String exchangeCreatedAt;       // 교환요청일자
    private String franchiseName;           // 교환요청지점
    private ExchangeReason reason;          // 교환사유
    private String exchangeManager;         // 교환요청담당자
    private String comment;                 // 처리 중 비고사항
    private String createdAt;               // 처리완료일자
    private ExchangeHistoryStatus status;   // 처리상태
    private String manager;                 // 처리담당자
    private Confirmed confirmed;            // 처리완료내역
    private List<ExchangeHistoryItemVO> exchangeHistoryItemList;    // 교환완료내역의 상품 리스트
}