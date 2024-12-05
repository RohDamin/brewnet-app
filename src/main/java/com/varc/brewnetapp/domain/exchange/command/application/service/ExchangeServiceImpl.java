package com.varc.brewnetapp.domain.exchange.command.application.service;

import com.varc.brewnetapp.common.S3ImageService;
import com.varc.brewnetapp.common.domain.approve.Confirmed;
import com.varc.brewnetapp.common.domain.drafter.DrafterApproved;
import com.varc.brewnetapp.common.domain.order.Available;
import com.varc.brewnetapp.domain.exchange.command.application.repository.*;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.entity.*;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.ex_entity.ExOrder;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.ex_entity.ExOrderItem;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.ex_entity.ExOrderItemCode;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.vo.ExchangeDrafterApproveReqVO;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.vo.ExchangeManagerApproveReqVO;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.vo.ExchangeReqItemVO;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.vo.ExchangeReqVO;
import com.varc.brewnetapp.common.domain.approve.Approval;
import com.varc.brewnetapp.common.domain.exchange.ExchangeStatus;
import com.varc.brewnetapp.domain.member.command.domain.aggregate.entity.Member;
import com.varc.brewnetapp.domain.member.command.domain.repository.MemberRepository;
import com.varc.brewnetapp.domain.storage.command.domain.aggregate.Stock;
import com.varc.brewnetapp.domain.storage.command.domain.repository.StockRepository;
import com.varc.brewnetapp.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service("ExchangeServiceCommand")
@RequiredArgsConstructor
@Slf4j
public class ExchangeServiceImpl implements ExchangeService {

    // 이후 의존성 수정 필요
    private final ExchangeRepository exchangeRepository;
    private final ExOrderRepository exOrderRepository;  // 임시
    private final ExOrderItemRepository exOrderItemRepository; // 임시
    private final MemberRepository memberRepository;
    private final ExchangeItemRepository exchangeItemRepository;
    private final ExchangeStatusHistoryRepository exchangeStatusHistoryRepository;
    private final com.varc.brewnetapp.domain.exchange.query.service.ExchangeServiceImpl exchangeServiceQuery;
    private final ExchangeApproverRepository exchangeApproverRepository;
    private final ExchangeStockHistoryRepository exchangeStockHistoryRepository;
    private final ExchangeItemStatusRepository exchangeItemStatusRepository;
    private final StockRepository stockRepository;
    private final ExchangeImgRepository exchangeImgRepository;
    private final S3ImageService s3ImageService;

    @Override
    @Transactional
    public Integer franCreateExchange(String loginId, ExchangeReqVO exchangeReqVO, List<MultipartFile> exchangeImageList) {
        /*TODO.
         * 교환 신청 시 변하는 상태값
         * [1] 교환 결재 상태           - tbl_exchange : approvalStatus = UNCONFIRMED
         * [2] 기안자의 교환 승인 여부    - tbl_exchange : drafterApproved = NONE
         * [3] 교환상태                - tbl_exchange_status_history : status = REQUESTED
         * [4] 반품/교환요청 가능여부     - tbl_order_item : available = UNAVAILABLE
         * */

        // 1. 해당 주문이 이 가맹점의 주문이 맞는지 확인
        ExOrder order = exOrderRepository.findById(exchangeReqVO.getOrderCode()).orElse(null);

        if (exchangeServiceQuery.isValidOrderByFranchise(loginId, exchangeReqVO.getOrderCode())) {

            // 2. 교환 객체 생성
            Exchange exchange = Exchange.builder()
                    .comment(null)
                    .createdAt(String.valueOf(LocalDateTime.now()))
                    .active(true)
                    .reason(exchangeReqVO.getReason())
                    .explanation(exchangeReqVO.getExplanation())
                    .approvalStatus(Approval.UNCONFIRMED)   // [1] 교환 결재 상태
                    .order(order)
                    .memberCode(null)
                    .delivery(null)
                    .drafterApproved(DrafterApproved.NONE)  // [2] 기안자의 교환 승인 여부
                    .sumPrice(exchangeReqVO.getSumPrice())
                    .build();

            exchangeRepository.save(exchange);


            for (ExchangeReqItemVO reqItem : exchangeReqVO.getExchangeItemList()) {
                // 3. 주문 별 상품 - 반품/교환요청 가능여부 변경
                // 복합키 객체 생성
                ExOrderItemCode exOrderItemCode = ExOrderItemCode.builder()
                        .orderCode(order.getOrderCode())            // 주문 코드 설정
                        .itemCode(reqItem.getItemCode())            // 상품 코드 설정
                        .build();

                ExOrderItem exOrderItem = exOrderItemRepository.findByOrderItemCode(exOrderItemCode)
                        .orElseThrow(() -> new ExchangeNotFoundException("존재하지 않는 주문 상품이 포함되어 있습니다."));
                if (exOrderItem.getAvailable() != Available.AVAILABLE) {
                    throw new IllegalArgumentException("교환 신청이 불가한 주문 상품이 포함되어 있습니다.");
                }

                exOrderItem = exOrderItem.toBuilder()
                        .available(Available.UNAVAILABLE)        // [4] 반품/교환요청 가능여부
                        .build();

                exOrderItemRepository.save(exOrderItem);

                // 4. 교환별상품 저장
                // 복합키 객체 생성
                ExchangeItemCode exchangeItemCode = ExchangeItemCode.builder()
                        .exchangeCode(exchange.getExchangeCode())   // 교환 코드 설정
                        .itemCode(reqItem.getItemCode())            // 상품 코드 설정
                        .build();

                // ExchangeItem 객체 생성
                ExchangeItem exchangeItem = ExchangeItem.builder()
                        .exchangeItemCode(exchangeItemCode)         // 복합키 설정
                        .quantity(exOrderItem.getQuantity())        // 기존 주문 별 상품의 수량 그대로 저장
                        .build();

                exchangeItemRepository.save(exchangeItem);

            }

            // 5. 교환상태이력 저장
            saveExchangeStatusHistory(ExchangeStatus.REQUESTED, exchange);

            // 6. 교환품목사진 저장
            for (MultipartFile image : exchangeImageList) {
                String s3Url = s3ImageService.upload(image);
                ExchangeImg exchangeImg = ExchangeImg.builder()
                        .imageUrl(s3Url)
                        .exchange(exchange)
                        .build();

                exchangeImgRepository.save(exchangeImg);
            }


            // 7. 교환코드(exchangeCode) 리턴 - 프론트엔드 상세페이지 이동 위해
            return exchange.getExchangeCode();

        } else {
            throw new UnauthorizedAccessException("로그인한 가맹점에서 작성한 주문에 대해서만 교환 요청할 수 있습니다");
        }
    }

    @Override
    @Transactional
    public void franCancelExchange(String loginId, Integer exchangeCode) {
        /*
         * 교환 취소 시 변하는 상태값
         * [1] 교환상태                - tbl_exchange_status_history : status = CANCELED (내역 추가됨)
         * [2] 활성화                  - tbl_exchange : active = False
         * [3] 반품/교환요청 가능여부     - tbl_order_item : available = AVAILABLE
         * */

        /*
         * 교환 취소 가능한 조건
         * 1. 교환 상태 이력(tbl_exchange_status_history)테이블의 교환상태(status)가 REQUESTED인 경우
         * */

        // 1. 해당 취소요청의 교환내역이 이 가맹점에서 작성한 것이 맞는지 확인
        if (exchangeServiceQuery.isValidExchangeByFranchise(loginId, exchangeCode)) {

            Exchange exchange = exchangeRepository.findById(exchangeCode)
                    .orElseThrow(() -> new ExchangeNotFoundException("교환 코드가 존재하지 않습니다."));

            ExchangeStatus exchangeStatus = exchangeServiceQuery.findExchangeLatestStatus(exchangeCode);

            // status가 REQUESTED인 경우에만 취소 가능
            if (exchangeStatus == ExchangeStatus.REQUESTED) {


                // 2. 교환 상태 이력(tbl_exchange_status_history)테이블에 취소내역 저장
                saveExchangeStatusHistory(ExchangeStatus.CANCELED, exchange);

                // 3. 교환(tbl_exchange) 테이블의 활성화(active)를 false로 변경
                exchange = exchange.toBuilder()     // 새 객체가 생성되지만, 영속성 컨텍스트는 아님
                        .active(false)              // [2] 활성화
                        .build();
                exchangeRepository.save(exchange);  //객체가 영속성 컨텍스트에 저장되고, 엔티티 매니저가 이 객체를 관리함


                // 4. 교환 별 상품 -> 주문 별 상품의 반품/교환요청 가능여부(available)을 AVAILABLE로 변경
                // 주문 별 상품(tbl_order_item)의
                // 교환 별 상품 리스트 -> 주문 별 상품 하나씩 조회 -> 상태값 변경
                List<ExchangeItem> exchangeItemList = exchangeItemRepository.findByExchangeItemCode_ExchangeCode(exchange.getExchangeCode());

                for (ExchangeItem exchangeItem : exchangeItemList) {

                    ExOrderItemCode orderItemCode = ExOrderItemCode.builder()
                            .orderCode(exchange.getOrder().getOrderCode())              // 주문 코드 설정
                            .itemCode(exchangeItem.getExchangeItemCode().getItemCode()) // 상품 코드 설정
                            .build();

                    ExOrderItem orderItem = exOrderItemRepository.findByOrderItemCode(orderItemCode)
                            .orElseThrow(() -> new ExchangeNotFoundException("교환할 상품이 존재하지 않습니다."));

                    orderItem = orderItem.toBuilder()
                            .available(Available.AVAILABLE) // [3] 반품/교환요청 가능여부
                            .build();
                    exOrderItemRepository.save(orderItem);
                }

            } else {
                throw new InvalidStatusException("교환신청 취소가 불가능합니다. 교환상태가 '교환요청'인 경우에만 취소할 수 있습니다");
            }
        } else {
            throw new UnauthorizedAccessException("로그인한 가맹점에서 작성한 교환요청에 대해서만 취소할 수 있습니다");
        }
    }

    @Override
    @Transactional
    public void drafterExchange(String loginId, int exchangeCode, ExchangeDrafterApproveReqVO exchangeApproveReqVO) {
        /*TODO.
         * 교환결재신청(최초기안자) 시 변하는 상태값
         * [1] 기안자의 교환 승인 여부    - tbl_exchange : drafter_approved = REJECT / APPROVE
         * [2] 교환 결재 상태            - tbl_exchange : approval_status =  UNCONFIRMED (변화 X)
         * [3] 교환상태                 - tbl_exchange_status_history : status = REJECTED / PENDING (내역 추가됨)
         * [4] 승인여부                 - tbl_exchange_approver : 기안자가 승인한 경우에만 UNCONFIRMED (결재자 등록됨)
         * */

        /*
         * 교환 결재 신청(최초기안자)
         * - 결재 신청이 가능한 조건:
         *   1. 교환상태이력(tbl_exchange_status_history) 테이블 교환상태(status) != REQUESTED
         *   2. 교환 결재 상태(approved) == UNCONFIRMED
         *   3. 기안자의 교환 승인 여부(drafter_approved) == NONE
         *   4. 교환 기안자(member_code) == null
         * */

        // 1. 교한 결재 신청이 가능한지 확인
        Exchange exchange = exchangeRepository.findById(exchangeCode)
                .orElseThrow(() -> new ExchangeNotFoundException("교환 코드가 존재하지 않습니다."));

        // 새로운 요청이거나 본인이 취소한 요청인 경우인지 확인
        ExchangeStatus status = exchangeServiceQuery.findExchangeLatestStatus(exchange.getExchangeCode());
        if (status != ExchangeStatus.REQUESTED && status != ExchangeStatus.PENDING) {
            throw new InvalidStatusException("결재신청이 불가능합니다. 교환 상태가 '교환요청'이 아닙니다.");
        } else if (exchange.getApprovalStatus() != Approval.UNCONFIRMED && exchange.getApprovalStatus() != Approval.CANCELED) {
            throw new InvalidStatusException("결재신청이 불가능합니다. 교환 결재 상태가 '미확인'이 아닙니다.");
        } else if (exchange.getDrafterApproved() != DrafterApproved.NONE) {
            throw new InvalidStatusException("결재신청이 불가능합니다. 이미 다른 관리자가 결재 등록했습니다.");
        } else if (exchange.getMemberCode() != null && !exchange.getMemberCode().getId().equals(loginId)) {
            throw new InvalidStatusException("결재신청이 불가능합니다. 이미 다른 관리자가 진행 중인 교환 내역입니다.");
        }

        // 2. 교환(tbl_exchange) 테이블 '기안자의 교환 승인 여부(drafter_approved)' 변경
        Member member = memberRepository.findById(loginId)
                .orElseThrow(() -> new MemberNotFoundException("해당 아이디의 회원이 존재하지 않습니다."));

        if (exchangeApproveReqVO.getApproval() == DrafterApproved.REJECT) {
            drafterRejectExchange(exchangeApproveReqVO, exchange, member);
        } else if (exchangeApproveReqVO.getApproval() == DrafterApproved.APPROVE) {
            drafterApproveExchange(exchangeApproveReqVO, exchange, member);
        } else {
            throw new InvalidStatusException("최초 기안자의 결재승인여부 값이 잘못되었습니다. 승인 또는 반려여야 합니다.");
        }
    }

    @Override
    @Transactional
    public void managerExchange(String loginId, int exchangeCode, ExchangeManagerApproveReqVO exchangeApproveReqVO) {
        /*
         * 교환결재(결재자) 시 변하는 상태값
         * [1] 교환 결재 상태            - tbl_exchange : approval_status = APPROVED / REJECTED
         * [2] 교환상태                 - tbl_exchange_status_history : status = APPROVED / REJECTED (내역 추가됨)
         * [3] 승인여부                 - tbl_exchange_approver : approved = APPROVED / REJECTED
         * [4] 결재일시                 - tbl_exchange_approver : created_at = 현재일시
         * */

        /*
         * 교환결재(결재자)
         * - 결재가 가능한 조건:
         *   1. 교환 별 결재자들(tbl_exchange_approver) 테이블 회원코드(member_code) == 현재 회원 코드
         *   2. 교환 별 결재자들(tbl_exchange_approver) 테이블 교환코드(exchange_code) == 교환 코드
         *   3. 교환 상태 이력 (tbl_exchange_status_history) 테이블 교환상태(status) == PENDING
         *   3. 교환 별 결재자들(tbl_exchange_approver) 테이블 승인여부(approved) == UNCONFIRMED
         * */

        // 1. 교환 결재가 가능한지 확인
        Member member = memberRepository.findById(loginId)
                .orElseThrow(() -> new IllegalArgumentException("멤버 코드가 존재하지 않습니다"));
        Exchange exchange = exchangeRepository.findById(exchangeCode)
                .orElseThrow(() -> new ExchangeNotFoundException("교환 코드가 존재하지 않습니다."));

        ExchangeApproverCode exchangeApproverCode = ExchangeApproverCode.builder()
                .exchangeCode(exchangeCode)
                .memberCode(member.getMemberCode())
                .build();
        ExchangeApprover exchangeApprover = exchangeApproverRepository.findById(exchangeApproverCode)
                .orElseThrow(() -> new IllegalArgumentException("\"결재가 불가능합니다. 결재 요청이 존재하지 않습니다."));

        ExchangeStatus status = exchangeServiceQuery.findExchangeLatestStatus(exchangeCode);
        if (status != ExchangeStatus.PENDING) {
            throw new InvalidStatusException("결재가 불가능합니다. 교환 상태가 '진행중'이 아닙니다.");
        } else if (exchange.getApprovalStatus() != Approval.UNCONFIRMED) {
            throw new InvalidStatusException("결재가 불가능합니다. 교환 결재 상태가 '미확인'이 아닙니다.");
        }


        if (exchangeApproveReqVO.getApproval() == Approval.APPROVED) {
            // 2. 교환(tbl_exchange) 테이블 '교환 결재 상태(approval_status)' 변경
            exchange = exchange.toBuilder()
                    .approvalStatus(Approval.APPROVED)
                    .build();
            exchangeRepository.save(exchange);

            // 3. 교환 상태 이력(tbl_exchange_status_history) 테이블 내역 추가
            saveExchangeStatusHistory(ExchangeStatus.APPROVED, exchange);

            // 4. 교환 별 결재자들(tbl_exchange_approver) 테이블 승인여부, 결재일시 변경
            exchangeApprover = exchangeApprover.toBuilder()
                    .approved(Approval.APPROVED)
                    .createdAt(String.valueOf(LocalDateTime.now()))
                    .comment(exchangeApproveReqVO.getComment())
                    .build();
            exchangeApproverRepository.save(exchangeApprover);


        } else if (exchangeApproveReqVO.getApproval() == Approval.REJECTED) {
            // 2. 교환(tbl_exchange) 테이블 '교환 결재 상태(approval_status)' 변경
            exchange = exchange.toBuilder()
                    .approvalStatus(Approval.REJECTED)
                    .build();
            exchangeRepository.save(exchange);

            // 3. 교환 상태 이력(tbl_exchange_status_history) 테이블 내역 추가
            saveExchangeStatusHistory(ExchangeStatus.REJECTED, exchange);

            // 4. 교환 별 결재자들(tbl_exchange_approver) 테이블 승인여부, 결재일시 변경
            exchangeApprover = exchangeApprover.toBuilder()
                    .approved(Approval.REJECTED)
                    .createdAt(String.valueOf(LocalDateTime.now()))
                    .comment(exchangeApproveReqVO.getComment())
                    .build();
            exchangeApproverRepository.save(exchangeApprover);
        } else {
            throw new IllegalArgumentException("결재자의 결재승인여부 값이 잘못되었습니다. 승인 또는 반려여야 합니다.");
        }
    }

    @Override
    @Transactional
    public void completeExchange(String loginId, int exchangeStockHistoryCode) {
        /*
         * 교환완료 시 변하는 상태값
         * [1] 교환상태                   - tbl_exchange_status_history : status = COMPLETED (내역 추가됨)
         * [2] 내역 확인 여부              - tbl_exchange_stock_history : confirmed = CONFIRMED
         * [3] 교환완료된 상품의 재고        - tbl_stock : available_stock 추가 (가용재고 추가됨)
         * [4] 반품/교환요청 가능여부        - tbl_order_item : available = AVAILABLE
         * */

        /*
         * 교환완료
         * - 교환완료가 가능한 조건:
         *   1. 교환 별 재고 처리 완료 내역(tbl_exchange_stock_history) 테이블 내역확인여부(confirmed) == UNCONFIRMED
         *
         * - 추가사항: 교환상태가 SHIPPED 아닐 때에도 완료처리 가능한 예외상황이 있을 수 있다고 판단해 교한상태로 완료가능 판단 X
         * */

        // 1. 교환 완료가 가능한지 확인
        ExchangeStockHistory exchangeStockHistory = exchangeStockHistoryRepository.findById(exchangeStockHistoryCode)
                .orElseThrow(() -> new IllegalArgumentException("타부서의 교환 별 재고 처리 완료 내역이 존재하지 않습니다."));
        if (exchangeStockHistory.getConfirmed() != Confirmed.UNCONFIRMED) {
            throw new IllegalArgumentException("교환 완료가 불가합니다. 이미 교환 완료된 내역입니다.");
        }

        // 2. 교환 상태 이력(tbl_exchange_status_history) 테이블 교환상태에 교환상태(status) COMPLETED 내역 추가
        saveExchangeStatusHistory(ExchangeStatus.COMPLETED, exchangeStockHistory.getExchange());

        // 3. 교환 별 재고 처리 완료 내역(tbl_exchange_stock_history) 테이블 내역확인여부(confirmed) CONFIRMED 변경
        exchangeStockHistory = exchangeStockHistory.toBuilder()
                .confirmed(Confirmed.CONFIRMED)
                .build();
        exchangeStockHistoryRepository.save(exchangeStockHistory);


        // 4. 재고(tbl_stock) 테이블 가용재고(available_stock) 증가

        // 4-1. 교환 별 재고 처리 완료 내역(tbl_exchange_stock_history) 테이블의 교환완료내역코드(exchange_stock_history_code)로
        //      교환 완료 상품 상태(tbl_exchange_item_status) 조회
        List<ExchangeItemStatus> exchangeItemStatusList = exchangeItemStatusRepository
                .findByExchangeItemStatusCode_ExchangeStockHistoryCode(exchangeStockHistory.getExchangeStockHistoryCode());

        // 4-2. 교환완료상품상태(tbl_exchange_item_status) 테이블의 상품코드(item_code)로 재고(tbl_stock) 조회
        //      창고코드(storage_code)=1인 재고 -> 임시로 창고코드 1인 재고만 조회
        for (ExchangeItemStatus exchangeItemStatus : exchangeItemStatusList) {
            Stock stock = stockRepository.findByStorageCodeAndItemCode(1, exchangeItemStatus.getExchangeItemStatusCode().getItemCode());

            // 4-4. 교환완료상품상태(tbl_exchange_item_status) 테이블의 재입고수량(restock_quantity) 만큼 재고(tbl_stock) 테이블의 가용재고(available_stock) 증가
            stock = stock.toBuilder()
                    .availableStock(stock.getAvailableStock() + exchangeItemStatus.getRestock_quantity())
                    .build();
            stockRepository.save(stock);
        }


        // 5. 주문 별 상품(tbl_order_item) 테이블 반품/교환요청 가능여부(available) AVAILABLE 변경
        List<ExOrderItem> exOrderItemList = exOrderItemRepository.findByOrderItemCode_orderCode(exchangeStockHistory.getExchange().getOrder().getOrderCode())
                .orElseThrow(() -> new OrderNotFound("완료 처리와 연관된 주문 별 상품 내역을 찾을 수 없습니다."));
        for (ExOrderItem exOrderItem : exOrderItemList) {
            exOrderItem = exOrderItem.toBuilder()
                    .available(Available.AVAILABLE)
                    .build();
            exOrderItemRepository.save(exOrderItem);
        }
    }


    private void drafterApproveExchange(ExchangeDrafterApproveReqVO exchangeApproveReqVO, Exchange exchange, Member member) {
        // [1] 교환(tbl_exchange) 테이블 '기안자의 교환 승인 여부(drafter_approved)'가 APPROVE 라면
        //      -> 교환(tbl_exchange) 테이블 '교환 결재 상태(approval_status)' = UNCONFIRMED (변화 X)
        //      -> 교환 상태 이력(tbl_exchange_status_history) 테이블 '교환 상태(status)' = PENDING (내역 추가됨)
        //   취소 X  -> 교환 별 결재자들(tbl_exchange_approver) 테이블 '승인여부(approved)' = APPROVED(기안자) / UNCONFIRMED(그 외 결재자)

        exchange = exchange.toBuilder()
                .drafterApproved(DrafterApproved.APPROVE)               // [1] 기안자의 교환 승인 여부
                .approvalStatus(Approval.UNCONFIRMED)                   // [2] 교환 결재 상태 (CANCELED 인 경우 UNCONFIRMED로 변경)
                .memberCode(member)                                     // 기안자 등록
                .comment(exchangeApproveReqVO.getComment())             // 첨언 등록
                .build();
        exchangeRepository.save(exchange);

        // 교환 상태 이력(tbl_exchange_status_history) 테이블 '교환 상태(status)' = PENDING (내역 추가됨)
        saveExchangeStatusHistory(ExchangeStatus.PENDING, exchange);

        // 교환 별 결재자들(tbl_exchange_approver) 테이블 '승인여부(approved)' = APPROVED (기안자만 결재자로 등록됨)
//        saveExchangeApprover(member.getMemberCode(), exchange, Approval.APPROVED, String.valueOf(LocalDateTime.now()), exchange.getComment());

        // 3-2. 결재자들 등록
        for (Integer approverCode : exchangeApproveReqVO.getApproverCodeList()) {
            saveExchangeApprover(approverCode, exchange, Approval.UNCONFIRMED, null, null);
        }
    }

    private void drafterRejectExchange(ExchangeDrafterApproveReqVO exchangeApproveReqVO, Exchange exchange, Member member) {
        // [1] 교환(tbl_exchange) 테이블 '기안자의 교환 승인 여부(drafter_approved)'가 REJECT 라면
        //      -> [2] 교환(tbl_exchange) 테이블 '교환 결재 상태(approval_status)' = REJECTED
        //      -> [3] 교환 상태 이력(tbl_exchange_status_history) 테이블 '교환 상태(status)' = REJECTED (내역 추가됨)
        //      -> [4] 교환 별 결재자들(tbl_exchange_approver) 테이블 '승인여부(approved)' = REJECTED (기안자만 결재자로 등록됨)

        exchange = exchange.toBuilder()
                .drafterApproved(DrafterApproved.REJECT)                // [1] 기안자의 교환 승인 여부
                .approvalStatus(Approval.REJECTED)                      // [2] 교환 결재 상태
                .memberCode(member)                                     // 기안자 등록
                .comment(exchangeApproveReqVO.getComment())             // 첨언 등록
                .build();
        exchangeRepository.save(exchange);

        // 교환 상태 이력(tbl_exchange_status_history) 테이블 '교환 상태(status)' = REJECTED (내역 추가됨)
        saveExchangeStatusHistory(ExchangeStatus.REJECTED, exchange);

        // 교환 별 결재자들(tbl_exchange_approver) 테이블 '승인여부(approved)' = REJECTED (기안자만 결재자로 등록됨)
//        saveExchangeApprover(member.getMemberCode(), exchange, Approval.REJECTED, String.valueOf(LocalDateTime.now()), exchange.getComment());
    }

    private void saveExchangeApprover(Integer member, Exchange exchange, Approval approval, String createdAt, String comment) {
        // 복합키 설정
        ExchangeApproverCode drafterApproverCode = ExchangeApproverCode.builder()
                .memberCode(member)                         // 멤버 코드 설정
                .exchangeCode(exchange.getExchangeCode())   // 교환 코드 설정
                .build();

        // 교환 별 결재자들 객체 생성
        ExchangeApprover approver = ExchangeApprover.builder()
                .exchangeApproverCode(drafterApproverCode)  // 복합키 설정
                .approved(approval)                         // [4] 교환 별 결재자들 (기안자)
                .createdAt(createdAt)
                .comment(comment)
                .active(true)
                .build();
        exchangeApproverRepository.save(approver);
    }

    private void saveExchangeStatusHistory(ExchangeStatus status, Exchange exchange) {
        ExchangeStatusHistory exchangeStatusHistory = ExchangeStatusHistory.builder()
                .status(status)   // [3] 교환 상태 이력
                .createdAt(String.valueOf(LocalDateTime.now()))
                .active(true)
                .exchange(exchange)
                .build();
        exchangeStatusHistoryRepository.save(exchangeStatusHistory);
    }

    @Override
    @Transactional
    public void cancelApprove(String loginId, int exchangeCode) {
        /*TODO.
         * 교환취소(결재자) 시 변하는 상태값
         * [1] 반품 결재 상태            - tbl_exchange : approval_status = UNCONFIRMED -> CANCELED
         * [1] 기안자의 교환 승인 여부    - tbl_exchange : drafter_approved = APPROVE -> NONE
         * [2] 교환상태                 - tbl_exchange_status_history : status = PENDING -> PENDING (변화 X)
         * [3] 결재 내역 삭제            - tbl_exchange_approver : Hard Delete
         * */

        /*
         * 교환 결재취소(결재자)
         * - 결재취소가 가능한 조건:
         *   1. 교환(tbl_exchange) 테이블 교환 기안자(member_code) != null
         *   2. 교환(tbl_exchange) 테이블 교환 기안자(member_code)가 해당 loginId를 갖는 멤버
         *   3. 교환(tbl_exchange) 테이블 기안자의 교환 승인 여부(drafter_approved) == APPROVE
         *   4. 교환(tbl_exchange) 테이블 교환결재상태(approval_status) == UNCONFIRMED
         *   5. 교환 상태 이력(tbl_exchange_status_history) 테이블 최신 교환상태(status) == PENDING
         *   6. 교환 별 결재자들(tbl_exchange_approver) 테이블 승인여부(approved) == UNCONFIRMED
         *      && 교환 별 결재자들(tbl_exchange_approver) 테이블 활성화(active) == true
         * */

        // 1. 결재취소가 가능한지 확인
        Exchange exchange = exchangeRepository.findById(exchangeCode)
                .orElseThrow(() -> new ExchangeNotFoundException("해당하는 교환 내역을 찾을 수 없습니다."));
        Member member = exchange.getMemberCode();
        ExchangeStatus exchangeStatus = exchangeServiceQuery.findExchangeLatestStatus(exchangeCode);

        List<ExchangeApprover> exchangeApproverList = exchangeApproverRepository.findByExchangeCode(exchangeCode)
                .orElseThrow(() -> new IllegalArgumentException("교환 별 결재자들을 찾을 수 없습니다"));

        if (member == null) {
            throw new MemberNotFoundException("교환 결재취소가 불가합니다. 아직 기안자가 없는 교환요청입니다.");
        } else if (!member.getId().equals(loginId)) {
            log.info("*** 0. member.getId:{}   loginId:{}", member.getId(), loginId);
            throw new IllegalArgumentException("교환 결재취소가 불가합니다. 기안자만 결재취소요청을 할 수 있습니다.");
        } else if (exchange.getDrafterApproved() != DrafterApproved.APPROVE) {
            throw new IllegalArgumentException("교환 결재취소가 불가합니다. 기안자의 교환 승인 여부가 '승인'인 경우에만 결재취소가 가능합니다.");
        } else if (exchange.getApprovalStatus() != Approval.UNCONFIRMED) {
            throw new IllegalArgumentException("교환 결재취소가 불가합니다. 상급자가 결재 요청을 아직 승인하지 않은 경우에만 결재취소가 가능합니다.");
        } else if (exchangeStatus != ExchangeStatus.PENDING) {
            throw new IllegalArgumentException("교환 결재취소가 불가합니다. 교환상태가 '진행중'인 경우에만 결재취소가 가능합니다.");
        } else if (exchangeApproverList.get(0).isActive() && exchangeApproverList.get(0).getApproved() != Approval.UNCONFIRMED) {
            // 활성화된 '교환 별 결재자들' 내역이 UNCONFIRMED 인 경우 결재취소 불가
            throw new IllegalArgumentException("교환 결재취소가 불가합니다. 상급자가 결재 요청을 아직 승인하지 않은 경우에만 결재취소가 가능합니다.");
        }

        // 2. 교환(tbl_exchange) 테이블 값 변경
        exchange = exchange.toBuilder()
                .approvalStatus(Approval.CANCELED)
                .drafterApproved(DrafterApproved.NONE)
                .build();
        exchangeRepository.save(exchange);

        // 3. 교환 별 결재자들(tbl_exchange_approver) 테이블 결재 내역 삭제
        exchangeApproverRepository.deleteById(exchangeApproverList.get(0).getExchangeApproverCode());

    }
}
