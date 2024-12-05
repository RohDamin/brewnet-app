package com.varc.brewnetapp.domain.returning.query.aggregate.vo;

import com.varc.brewnetapp.common.domain.approve.Confirmed;
import com.varc.brewnetapp.common.domain.returning.ReturningHistoryStatus;
import com.varc.brewnetapp.common.domain.returning.ReturningReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter // 품목 조회 위해 필요
public class ReturningHistoryDetailVO {
    private int returningStockHistoryCode;
    private int returningCode;
    private String returningCreatedAt;      // 반품요청일자
    private String franchiseName;           // 반품요청지점
    private ReturningReason reason;         // 반품사유
    private String returningManager;        // 반품요청담당자
    private String comment;                 // 처리 중 비고사항
    private String createdAt;               // 처리완료일자
    private ReturningHistoryStatus status;  // 처리상태
    private String manager;                 // 처리담당자
    private Confirmed confirmed;            // 처리완료여부
    private List<ReturningHistoryItemVO> returningHistoryItemList;    // 반품완료내역의 상품 리스트
}