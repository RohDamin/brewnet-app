package com.varc.brewnetapp.domain.item.query.controller;

import com.varc.brewnetapp.common.ResponseMessage;
import com.varc.brewnetapp.domain.item.query.dto.MustBuyItemDTO;
import com.varc.brewnetapp.domain.item.query.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/v1/franchise/items/must-buy")
public class FranchiseItemOrderQueryController {
    private final ItemService queryItemService;

    @Autowired
    public FranchiseItemOrderQueryController(ItemService queryItemService) {
        this.queryItemService = queryItemService;
    }

    @GetMapping
    @Operation(summary = "가맹점의 필수 구매 품목 리스트 조회")
    public ResponseEntity<ResponseMessage<List<MustBuyItemDTO>>> getMustBuyItems() {
        return ResponseEntity.ok(
                new ResponseMessage<>(
                        200, "OK", queryItemService.getMustBuyItemsForFranchise()
                )
        );
    }
}
