package com.varc.brewnetapp.domain.exchange.query.aggregate.vo;

import com.varc.brewnetapp.common.domain.approve.Approval;
import com.varc.brewnetapp.common.domain.position.Position;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ExchangeApproverVO {
    private int approverCode;
    private String approverName;        // 결재자(기안자)
    private Position position;          // 직급
    private Approval approval;          // 결재 처리 상태
    private String createdAt;           // 처리일자
    private String comment;             // 비고사항
}
