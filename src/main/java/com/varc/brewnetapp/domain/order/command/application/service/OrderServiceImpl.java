package com.varc.brewnetapp.domain.order.command.application.service;

import com.varc.brewnetapp.common.domain.drafter.DrafterApproved;
import com.varc.brewnetapp.common.domain.order.ApprovalStatus;
import com.varc.brewnetapp.common.domain.order.Available;
import com.varc.brewnetapp.common.domain.order.OrderHistoryStatus;
import com.varc.brewnetapp.common.domain.order.OrderApprovalStatus;
import com.varc.brewnetapp.domain.item.query.service.ItemService;
import com.varc.brewnetapp.domain.member.query.service.MemberService;
import com.varc.brewnetapp.domain.order.command.application.dto.DrafterRejectOrderRequestDTO;
import com.varc.brewnetapp.domain.order.command.application.dto.OrderApproveRequestDTO;
import com.varc.brewnetapp.domain.order.command.application.dto.OrderRequestApproveDTO;
import com.varc.brewnetapp.domain.order.command.application.dto.OrderApprovalRequestRejectDTO;
import com.varc.brewnetapp.domain.order.command.application.dto.orderrequest.OrderItemDTO;
import com.varc.brewnetapp.domain.order.command.application.dto.orderrequest.OrderRequestDTO;
import com.varc.brewnetapp.domain.order.command.application.dto.orderrequest.OrderRequestResponseDTO;
import com.varc.brewnetapp.domain.order.command.domain.aggregate.entity.*;
import com.varc.brewnetapp.domain.order.command.domain.aggregate.entity.compositionkey.OrderApprovalCode;
import com.varc.brewnetapp.domain.order.command.domain.aggregate.entity.compositionkey.OrderItemCode;
import com.varc.brewnetapp.domain.order.command.domain.repository.OrderApprovalRepository;
import com.varc.brewnetapp.domain.order.command.domain.repository.OrderItemRepository;
import com.varc.brewnetapp.domain.order.command.domain.repository.OrderRepository;
import com.varc.brewnetapp.domain.order.command.domain.repository.OrderStatusHistoryRepository;
import com.varc.brewnetapp.domain.order.query.service.OrderQueryService;
import com.varc.brewnetapp.domain.order.query.service.OrderValidateService;
import com.varc.brewnetapp.domain.storage.command.application.service.StorageService;
import com.varc.brewnetapp.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service(value = "commandOrderService")
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderApprovalRepository orderApprovalRepository;

    private final MemberService memberService;
    private final OrderQueryService orderQueryService;
    private final OrderValidateService orderValidateService;
    private final ItemService itemService;
    private final StorageService storageService;

    @Autowired
    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            OrderApprovalRepository orderApprovalRepository,
            MemberService memberService,
            OrderQueryService orderQueryService,
            OrderValidateService orderValidateService,
            ItemService itemService,
            StorageService storageService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.orderApprovalRepository = orderApprovalRepository;
        this.memberService = memberService;
        this.orderQueryService = orderQueryService;
        this.orderValidateService = orderValidateService;
        this.itemService = itemService;
        this.storageService = storageService;
    }

    // 가맹점의 주문요청
    @Transactional
    @Override
    public OrderRequestResponseDTO orderRequestByFranchise(
            OrderRequestDTO orderRequestDTO, String loginId
    ) {
        int requestFranchiseCode = memberService.getFranchiseInfoByLoginId(loginId).getFranchiseCode();
        List<OrderItemDTO> requestedOrderItemDTOList = orderRequestDTO.getOrderList();
        int orderedCode = orderRepository.save(
                Order.builder()
                        .createdAt(LocalDateTime.now())
                        .active(true)
                        .drafterApproved(DrafterApproved.NONE)
                        .approvalStatus(OrderApprovalStatus.UNCONFIRMED)
                        .sumPrice(getOrderTotalSum(requestedOrderItemDTOList))
                        .franchiseCode(requestFranchiseCode)
                        .build()
        ).getOrderCode();

        // 주문별 품목 추가
        addItemsPerOrder(orderedCode, requestedOrderItemDTOList);

        // 주문 총 합계 update
        orderRepository.save(
                Order.builder()
                        .orderCode(orderedCode)
                        .createdAt(LocalDateTime.now())
                        .active(true)
                        .drafterApproved(DrafterApproved.NONE)
                        .approvalStatus(OrderApprovalStatus.UNCONFIRMED)
                        .sumPrice(getOrderTotalSum(requestedOrderItemDTOList))
                        .franchiseCode(requestFranchiseCode)
                        .build()
        );

        // 주문 내역 수정
        recordOrderStatusHistory(orderedCode, OrderHistoryStatus.REQUESTED);

        return new OrderRequestResponseDTO(orderedCode);
    }

    // 주문 요청 취소
    @Transactional
    @Override
    public void cancelOrderRequest(Integer orderCode, Integer requestMemberFranchiseCode) {
        Order order = orderRepository.findById(orderCode).orElseThrow(() -> new OrderNotFound("Order not found"));

        int targetFranchiseCode = order.getFranchiseCode();

        // TODO: validate
        //  If the requester is from target franchise  [DONE]
        if (!Objects.equals(targetFranchiseCode, requestMemberFranchiseCode)) {
            throw new UnauthorizedAccessException(
                    "Unauthorized access." + "OrderCode: " + orderCode + " is from franchiseCode: " + targetFranchiseCode + ". Your franchise code is: " + requestMemberFranchiseCode
            );
        }

        // TODO: validate
        //  1. check if member_code in tbl_order is null and the order is valid(.active=1) [DONE]
        //  2. If the status column in tbl_order_status_history is 'REQUESTED'             [DONE]

        // TODO: Order Item 상태 수정
        //  3. tbl_order_item 목록 수정 available, active -> UNAVAILABLE, false              [DONE]

        if (orderValidateService.isOrderDrafted(orderCode)) {
            throw new OrderApprovalAlreadyExist("Order already drafted. Order code is: " + orderCode + ". Unable to cancel the order.");
        }

        String recentOrderStatus = orderQueryService.getOrderStatusHistoryByOrderCode(orderCode).getOrderHistoryStatus();
        if (!recentOrderStatus.equals(OrderHistoryStatus.REQUESTED.getValue())) {
            throw new UnexpectedOrderStatus("expected status: " + OrderHistoryStatus.REQUESTED + " but got " + recentOrderStatus);
        }

        updateOrderedItemListStatusTo(getOrderItemsByOrderCode(orderCode), Available.UNAVAILABLE);

        orderRepository.save(
                Order.builder()
                        .orderCode(order.getOrderCode())
                        .comment("가맹점의 주문 요청 취소")
                        .createdAt(order.getCreatedAt())
                        .active(order.isActive())
                        .approvalStatus(order.getApprovalStatus())
                        .drafterApproved(order.getDrafterApproved())
                        .sumPrice(order.getSumPrice())
                        .franchiseCode(order.getFranchiseCode())
                        .memberCode(order.getMemberCode())
                        .deliveryCode(order.getDeliveryCode())
                        .build()
        );

        recordOrderStatusHistory(orderCode, OrderHistoryStatus.CANCELED);
    }

    // 가맹점 주문 요청에 대한 일반 관리자의 상신
    @Transactional
    @Override
    public boolean requestApproveOrder(
            int orderCode,  // 주문 번호
            int memberCode, // 기안자 코드
            OrderApproveRequestDTO orderApproveRequestDTO   // 주문 상신 DTO
    ) {
        int targetManagerMemberCode = orderApproveRequestDTO.getSuperManagerMemberCode();

        Order order = orderRepository.findById(orderCode).orElseThrow(() -> new OrderNotFound("Order not found"));
//        List<OrderItem> orderItemList = getOrderItemsByOrderCode(order.getOrderCode());
        List<OrderApprover> orderApprover = orderApprovalRepository.findByOrderApprovalCode_OrderCode(order.getOrderCode());

        Integer presentOrderDrafterMemberCode = order.getMemberCode();
        log.debug("order.getMemberCode() - 기존 기안자: {}", presentOrderDrafterMemberCode);

        OrderApprovalStatus presentOrderStatus = order.getApprovalStatus();
        log.debug("order.getApprovalStatus() - 기존 주문 결재 상태: {}", presentOrderStatus);

        DrafterApproved presentDraftedApprovedStatus = order.getDrafterApproved();
        log.debug("order.getDrafterApproved() - 기존 기안자의 승인 상태: {}", presentDraftedApprovedStatus);

        // TODO: 일반 관리자의 상신
        //  - 상신된 주문 결재 요청이 있는지 확인 (validate)              [DONE]
        //  - 요청 대상자(책임 관리자)가 결재 라인에 있는지 확인 (validate)
        //  - tbl_order 수정                                      [DONE]
        //    - member_code(기안자) 할당된 바 있는지 확인 (validate)    [DONE]
        //    - member_code(기안자) 할당                            [DONE]
        //    - drafter_approved NONE -> APPROVE                 [DONE]
        //  - tbl_order_status_history 추가                       [DONE]
        //    - status - PENDING                                 [DONE]
        //  - tbl_order_approver 추가                             [DONE]
        //    - approved -> UNCONFIRMED                          [DONE]
        //  - tbl_order_item 수정                                 [DONE]
        //    - 해당 order_item의 available -> UNAVAILABLE         [DONE]

        if (order.getMemberCode() != null) {
            log.debug("orderApprover: {}", orderApprover);
            if (orderApprover.isEmpty()) {
                if (memberCode != order.getMemberCode()) {

                    // TODO: 앞서 결재가 취소된 경우                            [DONE]
                    //  상신 요청자가 취소한 사람(tbl_order.memberCode)인지 확인   [DONE]
                    log.debug("memberCode != order.getMemberCode()");
                    throw new UnauthorizedAccessException("재결재에 대한 권한이 없습니다.");
                }
                log.debug("memberCode == order.getMemberCode()");
            } else {
                throw new OrderApprovalAlreadyExist(
                        "order approval already exist. " +
                                "already requested by memberCode:" + order.getMemberCode() +
                                ", orderCode: " + order.getMemberCode()
                );
            }
        }

        orderRepository.save(
                Order.builder()
                        .orderCode(order.getOrderCode())
                        .comment(orderApproveRequestDTO.getComment()) // 상신 요청 첨언
                        .createdAt(order.getCreatedAt())
                        .active(order.isActive())
                        .approvalStatus(order.getApprovalStatus())
                        .drafterApproved(DrafterApproved.APPROVE)
                        .sumPrice(order.getSumPrice())
                        .franchiseCode(order.getFranchiseCode())
                        .memberCode(memberCode)     // 기안자
                        .build()
        );

        recordOrderStatusHistory(orderCode, OrderHistoryStatus.PENDING);

        orderApprovalRepository.save(
                OrderApprover.builder()
                        .orderApprovalCode(
                                OrderApprovalCode.builder()
                                        .orderCode(order.getOrderCode())
                                        .memberCode(targetManagerMemberCode)
                                        .build()
                        )
                        .approvalStatus(ApprovalStatus.UNCONFIRMED)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .active(true)
                        .build()
        );
        return true;
    }

    // 일반 관리자의 주문요청 상신 취소
    @Transactional
    @Override
    public boolean cancelOrderApproval(int orderCode, int memberCode) {
        Order order = orderRepository.findById(orderCode).orElseThrow(() -> new OrderNotFound("Order not found"));
        List<OrderApprover> orderApproval = orderApprovalRepository.findByOrderApprovalCode_OrderCode(orderCode);
        OrderStatusHistory optionalOrderStatusHistory = orderStatusHistoryRepository.findFirstByOrderCodeOrderByCreatedAtDesc(order.getOrderCode())
                .orElseThrow(() -> new OrderNotFound("Order not found"));

        if (orderApproval == null) {
            throw new ApprovalNotFoundException("취소할 주문 결재가 존재하지 않습니다.");
        }

        if(order.getMemberCode() != memberCode) {
            throw new AccessDeniedException("결재 취소 권한이 없습니다.");
        }

        if (!order.getApprovalStatus().equals(OrderApprovalStatus.UNCONFIRMED)) {
            throw new ApprovalAlreadyCompleted("이미 처리된 주문 기안입니다.");
        }

        if (!optionalOrderStatusHistory.getStatus().equals(OrderHistoryStatus.PENDING)) {
            throw new UnableToCancelApproval("주문 결재가 대기중이지 않습니다.");
        }

        // TODO: 주문요청 상신 취소
        //  - 취소할 결재건이 있는지 확인 (validate)
        //    - tbl_order_approver의 order_code 확인                                     [DONE]
        //    - tbl_order_approver의 approved가 UNCONFIRMED인지 확인                       [DONE]
        //    - tbl_order_approver의 active가 true인지 확인                                [DONE]
        //    - tbl_order의 member_code가 notnull인지 확인                                 [DONE]
        //    - tbl_order의 member_code가 상신 취소 요청자의 member_code와 같은지 확인           [DONE]
        //    - tbl_order의 approval_status가 UNCONFIRMED인지 확인                         [DONE]
        //    - tbl_order_status_history의 최신 status가 PENDING인지 확인                   [DONE]
        //  - 결재 취소로 인한 상태값 변경
        //    - tbl_order_approver의 order_code, member_code로 찾아 데이터 삭제(hard delete) [DONE]
        //    - tbl_order의 approval_status UNCONFIRMED -> CANCELED로 변경                [DONE]
        //    - tbl_order의 drafter_approved NONE으로 변경                                 [DONE]
        //    - tbl_order_status_history에 REQUESTED 추가                                 [DONE]

        orderApprovalRepository.deleteAll(orderApproval);
        orderRepository.save(
                Order.builder()
                        .orderCode(order.getOrderCode())
                        .comment(order.getComment())
                        .createdAt(order.getCreatedAt())
                        .active(order.isActive())
                        .approvalStatus(OrderApprovalStatus.CANCELED)
                        .drafterApproved(DrafterApproved.NONE)
                        .sumPrice(order.getSumPrice())
                        .franchiseCode(order.getFranchiseCode())
                        .memberCode(memberCode)
                        .deliveryCode(order.getDeliveryCode())
                        .build()
        );

        recordOrderStatusHistory(orderCode, OrderHistoryStatus.REQUESTED);

        return true;
    }

    // 주문요청 상신에 책임 관리자의 승인
    @Transactional
    @Override
    public boolean approveOrderDraft(int orderCode, int memberCode, OrderRequestApproveDTO orderRequestApproveDTO) {

        // TODO: 책임 관리자의 상신에 대한 승인 처리
        //  - tbl_order_approver 수정              [DONE]
        //    - approved UNCONFIRMED -> APPROVED  [DONE]
        //    - CREATED_AT -> 수정                 [DONE]
        //    - COMMENT -> 수정                    [DONE]
        //  - tbl_order 수정                       [DONE]
        //    - approval_status -> APPROVED       [DONE]
        //  - tbl_order_status_history 추가        [DONE]
        //    - STATUS -> APPROVED                [DONE]

        OrderApprover orderApprover = orderApprovalRepository.findById(
                OrderApprovalCode.builder()
                        .memberCode(memberCode)
                        .orderCode(orderCode)
                        .build()
        ).orElseThrow(() -> new OrderApprovalNotFound("Order approval not found"));
        log.info("책임 관리자의 승인 전 tbl_order_approver.approved: {}", orderApprover.getApprovalStatus());

        if (orderApprover.getApprovalStatus().equals(ApprovalStatus.APPROVED)) {
            throw new OrderDraftAlreadyApproved("order draft already approved. orderCode: " + orderCode + ", approvedManagerMemberCode: " + memberCode);
        }

        Order order = orderRepository.findById(orderCode).orElseThrow(() -> new OrderNotFound("Order not found"));
        log.info("책임 관리자의 승인 전 tbl_order.approval_status: {}", order.getApprovalStatus());
        log.info("책임 관리자의 승인 전 tbl_order.drafter_approved: {}", order.getDrafterApproved());

        String requestedComment = orderRequestApproveDTO.getComment();

        List<OrderItem> orderItemList = getOrderItemsByOrderCode(order.getOrderCode());

        orderApprovalRepository.save(
                OrderApprover.builder()
                        .orderApprovalCode(orderApprover.getOrderApprovalCode())
                        .approvalStatus(ApprovalStatus.APPROVED)
                        .createdAt(LocalDateTime.now())
                        .comment(requestedComment)
                        .active(orderApprover.isActive())
                        .build()
        );

        orderRepository.save(
                Order.builder()
                        .orderCode(order.getOrderCode())
                        .comment(order.getComment())
                        .createdAt(order.getCreatedAt())
                        .active(order.isActive())
                        .approvalStatus(OrderApprovalStatus.APPROVED)
                        .drafterApproved(order.getDrafterApproved())
                        .sumPrice(order.getSumPrice())
                        .franchiseCode(order.getFranchiseCode())
                        .memberCode(order.getMemberCode())
                        .build()
        );

        recordOrderStatusHistory(orderCode, OrderHistoryStatus.APPROVED);

        // TODO: 재고 변화
        //  현재 가용재고에 존재하는 재고에서 주문 요청 완료건에 대한 상품의 수 만큼 재고 수 변경 [DONE]
        //  시연을 위한 storageCode=1로, 추후 변경 필요
        orderItemList.forEach(
                orderItem -> storageService.setStockReadyToDepart(
                        1,      // 추후 변경 필요
                        orderItem.getOrderItemCode().getItemCode(),
                        orderItem.getQuantity()
                )
        );
        return true;
    }

    // 주문요청 상신에 책임 관리자의 반려
    @Transactional
    @Override
    public boolean rejectOrderDraft(int orderCode, int memberCode, OrderApprovalRequestRejectDTO orderApprovalRequestRejectDTO) {
        OrderApprover orderApprover = orderApprovalRepository.findById(OrderApprovalCode.builder()
                .memberCode(memberCode)
                .orderCode(orderCode)
                .build()).orElseThrow(() -> new OrderApprovalNotFound("Order approval not found"));
        Order order = orderRepository.findById(orderCode).orElseThrow(() -> new OrderNotFound("Order not found"));

        // TODO: 책임 관리자의 상신에 대한 반려 처리
        //  - validate                             [DONE]
        //    - 유효한 주문 상신인지 확인                [DONE]
        //  - tbl_order_approver 수정               [DONE]
        //    - approved UNCONFIRMED -> REJECTED   [DONE]
        //    - UPDATED_AT -> 수정                  [DONE]
        //    - COMMENT -> 수정                     [DONE]
        //  - tbl_order 수정                        [DONE]
        //    - approval_status -> REJECTED        [DONE]
        //  - tbl_order_status_history 추가         [DONE]
        //    - STATUS -> REJECTED                 [DONE]

        checkOrderApprovalIsValid(orderApprover, order);
        orderApprovalRepository.save(
                OrderApprover.builder()
                        .orderApprovalCode(
                                OrderApprovalCode.builder()
                                        .memberCode(memberCode)
                                        .orderCode(orderCode)
                                        .build()
                        )
                        .approvalStatus(ApprovalStatus.REJECTED)
                        .updatedAt(LocalDateTime.now())
                        .comment(orderApprovalRequestRejectDTO.getComment())
                        .active(true)
                        .build()
        );
        orderRepository.save(
                Order.builder()
                        .orderCode(orderCode)
                        .comment(order.getComment())
                        .createdAt(order.getCreatedAt())
                        .active(order.isActive())
                        .approvalStatus(OrderApprovalStatus.REJECTED)
                        .drafterApproved(order.getDrafterApproved())
                        .sumPrice(order.getSumPrice())
                        .franchiseCode(order.getFranchiseCode())
                        .memberCode(order.getMemberCode())
                        .deliveryCode(order.getDeliveryCode())
                        .build()
        );
        recordOrderStatusHistory(orderCode, OrderHistoryStatus.REJECTED);

        return true;
    }


    // 일반 관리자의 가맹점의 주문 요청 반려
    @Transactional
    public void rejectOrderByDrafter(int orderCode,
                                     DrafterRejectOrderRequestDTO drafterRejectOrderRequestDTO,
                                     String loginId) {

        // TODO: 가맹점의 주문 요청 반려
        //  - 주문 테이블에서 데이터 수정:                                             [DONE]
        //   tbl_order.APPROVAL_STATUS           - 'UNCONFIRMED' -> 'REJECTED'   [DONE]
        //   tbl_order.DRAFTER_APPROVED          - 'NONE' -> 'REJECT'            [DONE]
        //   tbl_order.COMMENT                   - 사유 입력                       [DONE]
        //   tbl_order.MEMBER_CODE               - 반려자(담당자) 코드 추가            [DONE]
        //  - 주문 상태 내역 테이블 수정:                                              [DONE]
        //   tbl_order_status_history.status     - 'REQUESTED' -> 'REJECTED'     [DONE]
        //   tbl_order_status_history.CREATED_AT                                 [DONE]
        //  - 주문 별 상품 테이블 수정:                                                [DONE]
        //   tbl_order_item.available            - 'AVAILABLE' -> 'UNAVAILABLE'  [DONE]

        String reason = drafterRejectOrderRequestDTO.getReason();
        int drafterMemberCode = memberService.getMemberByLoginId(loginId).getMemberCode();
        Order targetOrder = orderRepository.findById(orderCode).orElseThrow(() -> new OrderNotFound("Order not found"));
        List<OrderItem> orderItemList = orderItemRepository.findByOrderItemCode_OrderCode(orderCode);

        orderRepository.save(
                Order.builder()
                        .orderCode(targetOrder.getOrderCode())
                        .comment(reason)
                        .createdAt(targetOrder.getCreatedAt())
                        .active(targetOrder.isActive())
                        .approvalStatus(OrderApprovalStatus.REJECTED)
                        .drafterApproved(DrafterApproved.REJECT)
                        .sumPrice(targetOrder.getSumPrice())
                        .franchiseCode(targetOrder.getFranchiseCode())
                        .memberCode(drafterMemberCode)
                        .build()
        );
        recordOrderStatusHistory(orderCode, OrderHistoryStatus.REJECTED);
        updateOrderedItemListStatusTo(orderItemList, Available.UNAVAILABLE);
    }

    // TODO: 필수 구매 품목 지정

    // 주문 별 아이템 조회
    @Transactional
    public List<OrderItem> getOrderItemsByOrderCode(int orderCode) {
        List<OrderItem> orderItemList = orderItemRepository.findByOrderItemCode_OrderCode(orderCode);
        if (!orderItemList.isEmpty()) {
            return orderItemList;
        } else {
            throw new OrderItemNotFound("Order not found");
        }
    }

    // 주문 별 아이템 추가
    public void addItemsPerOrder(int orderedCode, List<OrderItemDTO> orderRequestRequestDTO) {

        if (!orderRequestRequestDTO.isEmpty()) {
            orderRequestRequestDTO.forEach(
                    orderItemDTO -> {
                        int itemCode = orderItemDTO.getItemCode();
                        int orderQuantity = orderItemDTO.getQuantity();
                        int partPriceSum = getPartSumPrice(itemCode, orderQuantity);

                        orderItemRepository.save(
                                OrderItem.builder()
                                        .orderItemCode(
                                                OrderItemCode.builder()
                                                        .itemCode(itemCode)
                                                        .orderCode(orderedCode)
                                                        .build()
                                        )
                                        .quantity(orderQuantity)
                                        .available(Available.UNAVAILABLE)
                                        .partSumPrice(partPriceSum)
                                        .build()
                        );
                    }
            );
        } else {
            throw new InvalidOrderItems("주문 아이템 최소 1개가 존재해야합니다. orderCode: " + orderedCode);
        }
    }

    // 주문 별 아이템 교환/반품 요청 가능 여부 수정
    public void updateOrderedItemListStatusTo(List<OrderItem> orderedItemList, Available availableStatus) {
        List<OrderItem> targetOrderedItemList = new ArrayList<>();
        orderedItemList.forEach(
                orderItem ->
                        targetOrderedItemList.add(
                                OrderItem.builder()
                                        .orderItemCode(orderItem.getOrderItemCode())
                                        .quantity(orderItem.getQuantity())
                                        .partSumPrice(orderItem.getPartSumPrice())
                                        .available(availableStatus)
                                        .build()
                        )
        );
        orderItemRepository.saveAll(targetOrderedItemList);
    }

    // 주문 내역 기록
    public void recordOrderStatusHistory(int orderCode, OrderHistoryStatus newStatus) {
        orderStatusHistoryRepository.save(
                OrderStatusHistory.builder()
                        .status(newStatus)
                        .createdAt(LocalDateTime.now())
                        .orderCode(orderCode)
                        .build()
        );
    }

    // 주문 합계 구하기
    private int getOrderTotalSum(List<OrderItemDTO> orderItemDTOList) {
        int totalSum = 0;
        for (OrderItemDTO orderItemDTO : orderItemDTOList) {

            /* TODO: itemCode로 Item 가격 찾기                                [DONE]
            *   int itemCode = orderItem.getItemCode();                    [DONE]
            *   int itemPrice = itemService.getItemPriceByCode(itemCode);  [DONE]
            */
            int itemCode = orderItemDTO.getItemCode();
            int quantity = orderItemDTO.getQuantity();

            totalSum += getPartSumPrice(itemCode, quantity);
        }
        return totalSum;
    }

    // 주문 아이템 부분 합 구하기
    private int getPartSumPrice(int itemCode, int quantity) {
        int sellingPrice = itemService.findItemSellingPriceByItemCode(itemCode);
        return sellingPrice * quantity;
    }

    // 주문 기안서 유효성 체크
    private void checkOrderApprovalIsValid(OrderApprover orderApprover, Order order) {
        int memberCode = orderApprover.getOrderApprovalCode().getMemberCode();
        int orderCode = orderApprover.getOrderApprovalCode().getOrderCode();
        OrderApprovalStatus presentOrderApprovalStatus = order.getApprovalStatus();

        if (order.getApprovalStatus() != OrderApprovalStatus.UNCONFIRMED ||
                orderApprover.getApprovalStatus() != ApprovalStatus.UNCONFIRMED){
            throw new ApprovalAlreadyCompleted("해당 주문 기안은 이미 처리되었습니다. 처리자 memberCode: " + memberCode + "해당 결재 승인 상태: " + presentOrderApprovalStatus);
        } else if (!orderApprover.isActive()) {
            throw new InvalidOrderApproval("유효하지 않은 기안입니다. " + "orderCode: " + orderCode + " memberCode: " + memberCode + "active: " + "false");
        }
    }
}
