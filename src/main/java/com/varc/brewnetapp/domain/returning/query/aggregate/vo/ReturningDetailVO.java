package com.varc.brewnetapp.domain.returning.query.aggregate.vo;

import com.varc.brewnetapp.common.domain.returning.ReturningReason;
import com.varc.brewnetapp.common.domain.returning.ReturningStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter // 품목 조회 위해 필요
public class ReturningDetailVO {
    private int returningCode;
    private int franchiseCode;
    private String createdAt;                        // 반품요청일자
    private String franchiseName;                    // 반품요청 지점명
    private ReturningReason reason;                  // 반품사유
    private String memberName;                       // 반품요청 담당자
    private String comment;                          // 비고사항
    private List<ReturningItemVO> returningItemList; // 반품 상품 리스트
    private List<String> returningImageList;         // 반품 이미지 리스트
    private String explanation;                      // 반품 상세사유
    private ReturningStatus status;                  // 반품상태
}
