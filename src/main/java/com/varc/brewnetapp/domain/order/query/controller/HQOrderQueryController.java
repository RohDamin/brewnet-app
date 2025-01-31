package com.varc.brewnetapp.domain.order.query.controller;

import com.varc.brewnetapp.common.ResponseMessage;

import com.varc.brewnetapp.common.SearchCriteria;
import com.varc.brewnetapp.domain.order.query.dto.*;
import com.varc.brewnetapp.domain.order.query.service.OrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/v1/hq/orders")
public class HQOrderQueryController {
    private final OrderQueryService orderQueryService;

    @Autowired
    public HQOrderQueryController(
            OrderQueryService orderQueryService
    ) {
        this.orderQueryService = orderQueryService;
    }

    @GetMapping("/list")
    @Operation(
            summary = "본사의 주문 리스트 조회",
            description = "sort: createdAtDesc, createdAtAsc, sumPriceDesc, sumPriceAsc"
    )
    public ResponseEntity<ResponseMessage<Page<HQOrderDTO>>> getOrderList(
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate
    ) {
        Page<HQOrderDTO> orderDTOList = orderQueryService.getOrderListForHQ(
                pageable,
                filter,
                sort,
                startDate,
                endDate
        );
        return ResponseEntity.ok(new ResponseMessage<>(200, "OK", orderDTOList));
    }

    @GetMapping("/excel")
    @Operation(summary = "엑셀 데이터 추출을 위한 주문 리스트 조회")
    public ResponseEntity<ResponseMessage<List<HQOrderDTO>>> getOrderListExcel(
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "criteria", required = false) SearchCriteria criteria,
            @RequestParam(name = "searchWord", required = false) String searchWord
    ) {
        List<HQOrderDTO> resultOrderDataDTO = orderQueryService.getExcelDataForHQBy(
                startDate,
                endDate,
                criteria,
                searchWord
        );

        return ResponseEntity.ok(
                new ResponseMessage<>(200, "OK", resultOrderDataDTO)
        );
    }

    @GetMapping("/search")
    @Operation(
            summary = "주문 일자(기간) 별로 검색 타입(주문번호, 주문지점, 주문담당자)에 따른 검색",
            description = "sort: createdAtDesc, createdAtAsc, sumPriceDesc, sumPriceAsc"
    )
    public ResponseEntity<ResponseMessage<Page<HQOrderDTO>>> getOrderListByHqSearch(
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "criteria", required = false) SearchCriteria criteria,
            @RequestParam(name = "searchWord", required = false) String searchWord

    ) {
        Page<HQOrderDTO> orderDTOList = orderQueryService.searchOrderListForHQ(
                pageable,
                filter,
                sort,
                startDate,
                endDate,
                criteria,
                searchWord
        );
        return ResponseEntity.ok(new ResponseMessage<>(200, "OK", orderDTOList));
    }

    @GetMapping("/detail/{orderCode}")
    @Operation(
            summary = "주문 코드를 path variable로 활용한 주문 상세 조회",
            description = "cf: doneDate는 orderStatus가 생성된 날짜이며, 가장 최신의 orderStatus만 가져온다."
    )
    public ResponseEntity<ResponseMessage<OrderDetailForHQDTO>> getOrderInformation(
            @PathVariable("orderCode") Integer orderCode) {
        OrderDetailForHQDTO orderDetailDTO = orderQueryService.getOrderDetailForHqBy(orderCode);
        return ResponseEntity.ok(new ResponseMessage<>(200, "OK", orderDetailDTO));
    }

    @GetMapping("/detail/{orderCode}/history")
    @Operation(summary = "본사의 주문 히스토리 조회")
    public ResponseEntity<ResponseMessage<List<OrderStatusHistory>>> healthcheck(
            @PathVariable("orderCode") Integer orderCode
    ) {
        List<OrderStatusHistory> orderHistoryList = orderQueryService.getOrderHistoryByOrderCode(orderCode);
        return ResponseEntity.ok(new ResponseMessage<>(200, "OK", orderHistoryList));
    }

    @GetMapping("/detail/{orderCode}/history/approval")
    @Operation(summary = "해당 주문건에 대한 본사의 결재 히스토리(내역) 조회")
    public ResponseEntity<ResponseMessage<List<OrderApprovalHistoryDTO>>> getApprovalHistories(
            @PathVariable("orderCode") Integer orderCode
    ) {
        List<OrderApprovalHistoryDTO> orderApprovalHistoryDTOList = orderQueryService.getOrderApprovalHistories(orderCode);
        return ResponseEntity.ok(new ResponseMessage<>(200, "OK", orderApprovalHistoryDTOList));
    }

    @GetMapping("/detail/{orderCode}/print/order-request")
    @Operation(summary = "주문 코드를 path variable로 활용한 주문요청서 출력데이터 조회")
    public ResponseEntity<ResponseMessage<OrderRequestDTO>> printOrderRequest(
            @PathVariable("orderCode") Integer orderCode) {
        OrderRequestDTO printedRequestedOrder = orderQueryService.printOrderRequest(orderCode);
        return ResponseEntity.ok(new ResponseMessage<>(200, "OK", printedRequestedOrder));
    }
}
