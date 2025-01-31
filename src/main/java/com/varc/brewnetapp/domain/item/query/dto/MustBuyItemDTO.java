package com.varc.brewnetapp.domain.item.query.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MustBuyItemDTO {
    private Integer mandatoryPurchaseCode;
    private int quantity;
    private String createdAt;
    private Boolean active;
    private Integer itemCode;
    private String itemName;
    private String dueDate;
    private Integer memberCode;
}
