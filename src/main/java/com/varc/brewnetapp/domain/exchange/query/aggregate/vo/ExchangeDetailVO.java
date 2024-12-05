package com.varc.brewnetapp.domain.exchange.query.aggregate.vo;

import com.varc.brewnetapp.common.domain.exchange.ExchangeStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter // 품목 조회 위해 필요
public class ExchangeDetailVO {
    private int exchangeCode;
    private int franchiseCode;
    private String createdAt;                       // 교환요청일자
    private String franchiseName;                   // 교환요청 지점명
    private String reason;                          // 교환사유
    private String memberName;                      // 교환요청 담당자
    private String comment;                         // 비고사항
    private List<ExchangeItemVO> exchangeItemList;  // 교환 상품 리스트
    private List<String> exchangeImageList;         // 교환 이미지 리스트
    private String explanation;                     // 교환 상세사유
    private ExchangeStatus status        ;          // 교환상태
}
