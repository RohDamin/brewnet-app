package com.varc.brewnetapp.domain.returning.query.aggregate.vo;

import com.varc.brewnetapp.common.domain.approve.Confirmed;
import com.varc.brewnetapp.common.domain.returning.ReturningReason;
import com.varc.brewnetapp.common.domain.returning.ReturningRefundStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter // 품목 조회 위해 필요
public class RefundHistoryDetailVO {
    private int returningRefundHistoryCode;
    private int returningCode;
    private String returningCreatedAt;      // 환불(반품)요청일자
    private String franchiseName;           // 환불(반품)요청지점
    private ReturningReason reason;         // 환불(반품)사유
    private String returningManager;        // 환불(반품)요청담당자
    private String comment;                 // 처리 중 비고사항
    private String createdAt;               // 처리완료일자
    private ReturningRefundStatus status;   // 처리상태
    private String manager;                 // 처리담당자
    private Confirmed confirmed;            // 처리완료여부
    private List<RefundHistoryItemVO> refundHistoryItemList;    // 환불완료내역의 상품 리스트
}