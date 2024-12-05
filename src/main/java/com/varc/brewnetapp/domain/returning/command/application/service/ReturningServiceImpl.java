package com.varc.brewnetapp.domain.returning.command.application.service;

import com.varc.brewnetapp.common.S3ImageService;
import com.varc.brewnetapp.common.domain.approve.Approval;
import com.varc.brewnetapp.common.domain.approve.Confirmed;
import com.varc.brewnetapp.common.domain.drafter.DrafterApproved;
import com.varc.brewnetapp.common.domain.exchange.ExchangeStatus;
import com.varc.brewnetapp.common.domain.order.Available;
import com.varc.brewnetapp.common.domain.returning.ReturningStatus;
import com.varc.brewnetapp.domain.exchange.command.application.repository.ExOrderItemRepository;
import com.varc.brewnetapp.domain.exchange.command.application.repository.ExOrderRepository;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.entity.ExchangeItemStatus;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.entity.ExchangeStockHistory;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.ex_entity.ExOrder;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.ex_entity.ExOrderItem;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.ex_entity.ExOrderItemCode;
import com.varc.brewnetapp.domain.member.command.domain.aggregate.entity.Member;
import com.varc.brewnetapp.domain.member.command.domain.repository.MemberRepository;
import com.varc.brewnetapp.domain.returning.command.domain.aggregate.entity.*;
import com.varc.brewnetapp.domain.returning.command.domain.aggregate.entity.compositionkey.ReturningApproverCode;
import com.varc.brewnetapp.domain.returning.command.domain.aggregate.entity.compositionkey.ReturningItemCode;
import com.varc.brewnetapp.domain.returning.command.domain.aggregate.vo.ReturningDrafterApproveReqVO;
import com.varc.brewnetapp.domain.returning.command.domain.aggregate.vo.ReturningManagerApproveReqVO;
import com.varc.brewnetapp.domain.returning.command.domain.aggregate.vo.ReturningReqItemVO;
import com.varc.brewnetapp.domain.returning.command.domain.aggregate.vo.ReturningReqVO;
import com.varc.brewnetapp.domain.returning.command.domain.repository.*;
import com.varc.brewnetapp.domain.storage.command.domain.aggregate.Stock;
import com.varc.brewnetapp.domain.storage.command.domain.repository.StockRepository;
import com.varc.brewnetapp.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;


@Service("ReturningServiceCommand")
@RequiredArgsConstructor
@Slf4j
public class ReturningServiceImpl implements ReturningService {

    private final ReturningRepository returningRepository;
    private final com.varc.brewnetapp.domain.returning.query.service.ReturningServiceImpl returningServiceQuery;
    private final ExOrderRepository exOrderRepository;  // 임시
    private final ExOrderItemRepository exOrderItemRepository; // 임시
    private final MemberRepository memberRepository;
    private final ReturningItemRepository returningItemRepository;
    private final ReturningStatusHistoryRepository returningStatusHistoryRepository;
    private final ReturningImgRepository returningImgRepository;
    private final ReturningApproverRepository returningApproverRepository;
    private final ReturningStockHistoryRepository returningStockHistoryRepository;
    private final ReturningRefundHistoryRepository returningRefundHistoryRepository;
    private final ReturningItemStatusRepository returningItemStatusRepository;
    private final StockRepository stockRepository;
    private final S3ImageService s3ImageService;


    @Override
    @Transactional
    public Integer franCreateReturning(String loginId, ReturningReqVO returningReqVO, List<MultipartFile> returningImageList) {
        /*TODO.
         * 반품 신청 시 변하는 상태값
         * [1] 반품 결재 상태           - tbl_return : approvalStatus = UNCONFIRMED
         * [2] 기안자의 반품 승인 여부    - tbl_return : drafterApproved = NONE
         * [3] 반품상태                - tbl_return_status_history : status = REQUESTED
         * [4] 반품/교환요청 가능여부     - tbl_order_item : available = UNAVAILABLE
         * */

        ExOrder order = exOrderRepository.findById(returningReqVO.getOrderCode()).orElse(null);

        if (returningServiceQuery.isValidOrderByFranchise(loginId, returningReqVO.getOrderCode())) {

            // 2. 반품 객체 생성
            Returning returning = Returning.builder()
                    .comment(null)
                    .createdAt(String.valueOf(LocalDateTime.now()))
                    .active(true)
                    .reason(returningReqVO.getReason())
                    .explanation(returningReqVO.getExplanation())
                    .approvalStatus(Approval.UNCONFIRMED)   // [1] 반품 결재 상태
                    .order(order)
                    .memberCode(null)
                    .delivery(null)
                    .drafterApproved(DrafterApproved.NONE)  // [2] 기안자의 반품 승인 여부
                    .sumPrice(returningReqVO.getSumPrice())
                    .build();

            returningRepository.save(returning);


            for (ReturningReqItemVO reqItem : returningReqVO.getReturningItemList()) {
                // 3. 주문 별 상품 - 반품/교환요청 가능여부 변경
                // 복합키 객체 생성
                ExOrderItemCode exOrderItemCode = ExOrderItemCode.builder()
                        .orderCode(order.getOrderCode())            // 주문 코드 설정
                        .itemCode(reqItem.getItemCode())            // 상품 코드 설정
                        .build();

                ExOrderItem exOrderItem = exOrderItemRepository.findByOrderItemCode(exOrderItemCode)
                        .orElseThrow(() -> new ReturningNotFoundException("존재하지 않는 주문 상품이 포함되어 있습니다."));
                if (exOrderItem.getAvailable() != Available.AVAILABLE) {
                    throw new IllegalArgumentException("반품 신청이 불가한 주문 상품이 포함되어 있습니다.");
                }

                exOrderItem = exOrderItem.toBuilder()
                        .available(Available.UNAVAILABLE)        // [4] 반품/교환요청 가능여부
                        .build();

                exOrderItemRepository.save(exOrderItem);

                // 4. 반품별상품 저장
                // 복합키 객체 생성
                ReturningItemCode returningItemCode = ReturningItemCode.builder()
                        .returningCode(returning.getReturningCode())    // 반품 코드 설정
                        .itemCode(reqItem.getItemCode())                // 상품 코드 설정
                        .build();

                // ReturningItem 객체 생성
                ReturningItem returningItem = ReturningItem.builder()
                        .returningItemCode(returningItemCode)           // 복합키 설정
                        .quantity(exOrderItem.getQuantity())            // 기존 주문 별 상품의 수량 그대로 저장
                        .build();

                returningItemRepository.save(returningItem);

            }

            // 5. 반품상태이력 저장
            saveReturningStatusHistory(ReturningStatus.REQUESTED, returning);

            // 6. 반품품목사진 저장
            for (MultipartFile image : returningImageList) {
                String s3Url = s3ImageService.upload(image);
                ReturningImg returningImg = ReturningImg.builder()
                        .imageUrl(s3Url)
                        .returning(returning)
                        .build();

                returningImgRepository.save(returningImg);
            }


            // 7. 반품코드(returningCode) 리턴 - 프론트엔드 상세페이지 이동 위해
            return returning.getReturningCode();

        } else {
            throw new UnauthorizedAccessException("로그인한 가맹점에서 작성한 주문에 대해서만 반품 요청할 수 있습니다");
        }
    }

    @Override
    @Transactional
    public void franCancelReturning(String loginId, Integer returningCode) {
        /*TODO.
         * 반품 취소 시 변하는 상태값
         * [1] 반품상태                - tbl_return_status_history : status = CANCELED (내역 추가됨)
         * [2] 활성화                  - tbl_return : active = False
         * [3] 반품/교환요청 가능여부     - tbl_order_item : available = AVAILABLE
         * */

        /*
         * 반품 취소 가능한 조건
         * 1. 반품 상태 이력(tbl_return_status_history)테이블의 반품상태(status)가 REQUESTED인 경우
         * */

        // 1. 해당 취소요청의 반품내역이 이 가맹점에서 작성한 것이 맞는지 확인
        if (returningServiceQuery.isValidReturningByFranchise(loginId, returningCode)) {

            Returning returning = returningRepository.findById(returningCode)
                    .orElseThrow(() -> new ReturningNotFoundException("반품 코드가 존재하지 않습니다."));

            ReturningStatus returningStatus = returningServiceQuery.findReturningLatestStatus(returningCode);

            // status가 REQUESTED인 경우에만 취소 가능
            if (returningStatus == ReturningStatus.REQUESTED) {


                // 2. 반품 상태 이력(tbl_return_status_history)테이블에 취소내역 저장
                saveReturningStatusHistory(ReturningStatus.CANCELED, returning);

                // 3. 반품(tbl_return) 테이블의 활성화(active)를 false로 변경
                returning = returning.toBuilder()     // 새 객체가 생성되지만, 영속성 컨텍스트는 아님
                        .active(false)              // [2] 활성화
                        .build();
                returningRepository.save(returning);  //객체가 영속성 컨텍스트에 저장되고, 엔티티 매니저가 이 객체를 관리함


                // 4. 반품 별 상품 -> 주문 별 상품의 반품/교환요청 가능여부(available)을 AVAILABLE로 변경
                // 주문 별 상품(tbl_order_item)의
                // 반품 별 상품 리스트 -> 주문 별 상품 하나씩 조회 -> 상태값 변경
                List<ReturningItem> returningItemList = returningItemRepository.findByReturningItemCode_ReturningCode(returning.getReturningCode());

                for (ReturningItem returningItem : returningItemList) {

                    ExOrderItemCode orderItemCode = ExOrderItemCode.builder()
                            .orderCode(returning.getOrder().getOrderCode())              // 주문 코드 설정
                            .itemCode(returningItem.getReturningItemCode().getItemCode()) // 상품 코드 설정
                            .build();

                    ExOrderItem orderItem = exOrderItemRepository.findByOrderItemCode(orderItemCode)
                            .orElseThrow(() -> new ReturningNotFoundException("반품할 상품이 존재하지 않습니다."));

                    orderItem = orderItem.toBuilder()
                            .available(Available.AVAILABLE) // [3] 반품/교환요청 가능여부
                            .build();
                    exOrderItemRepository.save(orderItem);
                }

            } else {
                throw new InvalidStatusException("반품신청 취소가 불가능합니다. 반품상태가 '반품요청'인 경우에만 취소할 수 있습니다");
            }
        } else {
            throw new UnauthorizedAccessException("로그인한 가맹점에서 작성한 반품요청에 대해서만 취소할 수 있습니다");
        }
    }

    @Override
    @Transactional
    public void drafterReturning(String loginId, int returningCode, ReturningDrafterApproveReqVO returningApproveReqVO) {
        /*TODO.
         * 반품결재신청(최초기안자) 시 변하는 상태값
         * [1] 기안자의 반품 승인 여부    - tbl_return : drafter_approved = REJECT / APPROVE
         * [2] 반품 결재 상태            - tbl_return : approval_status =  UNCONFIRMED (변화 X)
         * [3] 반품상태                 - tbl_return_status_history : status = REJECTED / PENDING (내역 추가됨)
         * [4] 승인여부                 - tbl_return_approver : approved = REJECTED / APPROVED & UNCONFIRMED (결재자 등록됨)
         * */

        /*
         * 반품 결재 신청(최초기안자)
         * - 결재 신청이 가능한 조건:
         *   1. 반품상태이력(tbl_return_status_history) 테이블 반품상태(status) != REQUESTED
         *   2. 반품 결재 상태(approved) == UNCONFIRMED
         *   3. 기안자의 반품 승인 여부(drafter_approved) == NONE
         *   4. 반품 기안자(member_code) == null
         * */

        // 1. 반품 결재 신청이 가능한지 확인
        Returning returning = returningRepository.findById(returningCode)
                .orElseThrow(() -> new ReturningNotFoundException("반품 코드가 존재하지 않습니다."));

        // 새로운 요청이거나 본인이 취소한 요청인 경우인지 확인
        ReturningStatus status = returningServiceQuery.findReturningLatestStatus(returning.getReturningCode());
        if (status != ReturningStatus.REQUESTED  && status != ReturningStatus.PENDING) {
            throw new InvalidStatusException("결재신청이 불가능합니다. 반품 상태가 '반품요청'이 아닙니다.");
        } else if (returning.getApprovalStatus() != Approval.UNCONFIRMED  && returning.getApprovalStatus() != Approval.CANCELED) {
            throw new InvalidStatusException("결재신청이 불가능합니다. 반품 결재 상태가 '미확인'이 아닙니다.");
        } else if (returning.getDrafterApproved() != DrafterApproved.NONE) {
            throw new InvalidStatusException("결재신청이 불가능합니다. 이미 다른 관리자가 결재 등록했습니다.");
        } else if (returning.getMemberCode() != null && !returning.getMemberCode().getId().equals(loginId)) {
            throw new InvalidStatusException("결재신청이 불가능합니다. 이미 다른 관리자가 진행 중인 반품 내역입니다.");
        }

        // 2. 반품(tbl_return) 테이블 '기안자의 반품 승인 여부(drafter_approved)' 변경
        Member member = memberRepository.findById(loginId)
                .orElseThrow(() -> new MemberNotFoundException("해당 아이디의 회원이 존재하지 않습니다."));

        if (returningApproveReqVO.getApproval() == DrafterApproved.REJECT) {
            drafterRejectReturning(returningApproveReqVO, returning, member);
        } else if (returningApproveReqVO.getApproval() == DrafterApproved.APPROVE) {
            drafterApproveReturning(returningApproveReqVO, returning, member);
        } else {
            throw new InvalidStatusException("최초 기안자의 결재승인여부 값이 잘못되었습니다. 승인 또는 반려여야 합니다.");
        }
    }

    @Override
    @Transactional
    public void managerReturning(String loginId, int returningCode, ReturningManagerApproveReqVO returningApproveReqVO) {
        /*TODO.
         * 반품결재(결재자) 시 변하는 상태값
         * [1] 반품 결재 상태            - tbl_return : approval_status = APPROVED / REJECTED
         * [2] 반품상태                 - tbl_return_status_history : status = APPROVED / REJECTED (내역 추가됨)
         * [3] 승인여부                 - tbl_return_approver : approved = APPROVED / REJECTED
         * [4] 결재일시                 - tbl_return_approver : created_at = 현재일시
         * */

        /*
         * 반품결재(결재자)
         * - 결재가 가능한 조건:
         *   1. 반품 별 결재자들(tbl_return_approver) 테이블 회원코드(member_code) == 현재 회원 코드
         *   2. 반품 별 결재자들(tbl_return_approver) 테이블 반품코드(return_code) == 반품 코드
         *   3. 반품 상태 이력 (tbl_return_status_history) 테이블 반품상태(status) == PENDING
         *   3. 반품 별 결재자들(tbl_return_approver) 테이블 승인여부(approved) == UNCONFIRMED
         * */

        // 1. 반품 결재가 가능한지 확인
        Member member = memberRepository.findById(loginId)
                .orElseThrow(() -> new IllegalArgumentException("멤버 코드가 존재하지 않습니다"));
        Returning returning = returningRepository.findById(returningCode)
                .orElseThrow(() -> new ReturningNotFoundException("반품 코드가 존재하지 않습니다."));

        ReturningApproverCode returningApproverCode = ReturningApproverCode.builder()
                .returningCode(returningCode)
                .memberCode(member.getMemberCode())
                .build();
        ReturningApprover returningApprover = returningApproverRepository.findById(returningApproverCode)
                .orElseThrow(() -> new IllegalArgumentException("결재가 불가능합니다. 결재 요청이 존재하지 않습니다."));

        ReturningStatus status = returningServiceQuery.findReturningLatestStatus(returningCode);
        if (status != ReturningStatus.PENDING) {
            throw new InvalidStatusException("결재가 불가능합니다. 반품 상태가 '진행중'이 아닙니다.");
        } else if (returning.getApprovalStatus() != Approval.UNCONFIRMED) {
            throw new InvalidStatusException("결재가 불가능합니다. 반품 결재 상태가 '미확인'이 아닙니다.");
        }


        if (returningApproveReqVO.getApproval() == Approval.APPROVED) {
            // 2. 반품(tbl_return) 테이블 '반품 결재 상태(approval_status)' 변경
            returning = returning.toBuilder()
                    .approvalStatus(Approval.APPROVED)
                    .build();
            returningRepository.save(returning);

            // 3. 반품 상태 이력(tbl_return_status_history) 테이블 내역 추가
            saveReturningStatusHistory(ReturningStatus.APPROVED, returning);

            // 4. 반품 별 결재자들(tbl_return_approver) 테이블 승인여부, 결재일시 변경
            returningApprover = returningApprover.toBuilder()
                    .approved(Approval.APPROVED)
                    .createdAt(String.valueOf(LocalDateTime.now()))
                    .comment(returningApproveReqVO.getComment())
                    .build();
            returningApproverRepository.save(returningApprover);


        } else if (returningApproveReqVO.getApproval() == Approval.REJECTED) {
            // 2. 반품(tbl_return) 테이블 '반품 결재 상태(approval_status)' 변경
            returning = returning.toBuilder()
                    .approvalStatus(Approval.REJECTED)
                    .build();
            returningRepository.save(returning);

            // 3. 반품 상태 이력(tbl_return_status_history) 테이블 내역 추가
            saveReturningStatusHistory(ReturningStatus.REJECTED, returning);

            // 4. 반품 별 결재자들(tbl_return_approver) 테이블 승인여부, 결재일시 변경
            returningApprover = returningApprover.toBuilder()
                    .approved(Approval.REJECTED)
                    .createdAt(String.valueOf(LocalDateTime.now()))
                    .comment(returningApproveReqVO.getComment())
                    .build();
            returningApproverRepository.save(returningApprover);
        } else {
            throw new IllegalArgumentException("결재자의 결재승인여부 값이 잘못되었습니다. 승인 또는 반려여야 합니다.");
        }
    }

    @Override
    @Transactional
    public void cancelApprove(String loginId, int returningCode) {
        /*TODO.
         * 반품취소(결재자) 시 변하는 상태값
         * [1] 반품 결재 상태            - tbl_return : approval_status = UNCONFIRMED -> CANCELED
         * [1] 기안자의 반품 승인 여부    - tbl_return : drafter_approved = APPROVE -> NONE
         * [2] 반품상태                 - tbl_return_status_history : status = PENDING -> PENDING (변화 X)
         * [3] 결재 내역 삭제            - tbl_return_approver : Hard Delete
         * */

        /*
         * 반품 결재취소(결재자)
         * - 결재취소가 가능한 조건:
         *   1. 반품(tbl_return) 테이블 반품 기안자(member_code) != null
         *   2. 반품(tbl_return) 테이블 반품 기안자(member_code)가 해당 loginId를 갖는 멤버
         *   3. 반품(tbl_return) 테이블 기안자의 반품 승인 여부(drafter_approved) == APPROVE
         *   4. 반품(tbl_return) 테이블 반품결재상태(approval_status) == UNCONFIRMED
         *   5. 반품 상태 이력(tbl_return_status_history) 테이블 최신 반품상태(status) == PENDING
         *   6. 반품 별 결재자들(tbl_return_approver) 테이블 승인여부(approved) == UNCONFIRMED
         *      && 반품 별 결재자들(tbl_return_approver) 테이블 활성화(active) == true
         * */

        // 1. 결재취소가 가능한지 확인
        Returning returning = returningRepository.findById(returningCode)
                .orElseThrow(() -> new ReturningNotFoundException("해당하는 반품 내역을 찾을 수 없습니다."));
        Member member = returning.getMemberCode();
        ReturningStatus returningStatus = returningServiceQuery.findReturningLatestStatus(returningCode);

//        ReturningApproverCode returningApproverCode = ReturningApproverCode.builder()
////                .memberCode(member.getMemberCode())
//                .returningCode(returningCode)
//                .build();
        List<ReturningApprover> returningApproverList = returningApproverRepository.findByReturningCode(returningCode)
                .orElseThrow(() -> new IllegalArgumentException("반품 별 결재자들을 찾을 수 없습니다"));

        if (member == null) {
            throw new MemberNotFoundException("반품 결재취소가 불가합니다. 아직 기안자가 없는 반품요청입니다.");
        } else if (!member.getId().equals(loginId)) {
            log.info("*** 0. member.getId:{}   loginId:{}", member.getId(), loginId);
            throw new IllegalArgumentException("반품 결재취소가 불가합니다. 기안자만 결재취소요청을 할 수 있습니다.");
        } else if (returning.getDrafterApproved() != DrafterApproved.APPROVE) {
            throw new IllegalArgumentException("반품 결재취소가 불가합니다. 기안자의 반품 승인 여부가 '승인'인 경우에만 결재취소가 가능합니다.");
        } else if (returning.getApprovalStatus() != Approval.UNCONFIRMED) {
            throw new IllegalArgumentException("반품 결재취소가 불가합니다. 상급자가 결재 요청을 아직 승인하지 않은 경우에만 결재취소가 가능합니다.");
        } else if (returningStatus != ReturningStatus.PENDING) {
            throw new IllegalArgumentException("반품 결재취소가 불가합니다. 반품상태가 '진행중'인 경우에만 결재취소가 가능합니다.");
        } else if (returningApproverList.get(0).isActive() && returningApproverList.get(0).getApproved() != Approval.UNCONFIRMED) {
            // 활성화된 '반품 별 결재자들' 내역이 UNCONFIRMED 인 경우 결재취소 불가
            throw new IllegalArgumentException("반품 결재취소가 불가합니다. 상급자가 결재 요청을 아직 승인하지 않은 경우에만 결재취소가 가능합니다.");
        }

        // 2. 반품(tbl_return) 테이블 값 변경
        returning = returning.toBuilder()
                .approvalStatus(Approval.CANCELED)
                .drafterApproved(DrafterApproved.NONE)
                .build();
        returningRepository.save(returning);

        // 3. 반품 별 결재자들(tbl_return_approver) 테이블 결재 내역 삭제
        returningApproverRepository.deleteById(returningApproverList.get(0).getReturningApproverCode());
    }

    @Override
    @Transactional
    public void completeStock(String loginId, int returningStockHistoryCode) {
        /*TODO.
         * 재고처리(반품)만 완료인 경우 변하는 상태값
         * [1] 내역 확인 여부              - tbl_return_stock_history : confirmed = CONFIRMED
         * */

        /*
         * 재고처리 완료
         * - 재고처리 완료가 가능한 조건:
         *   1. 반품 별 재고 처리 완료 내역(tbl_return_stock_history) 테이블 내역확인여부(confirmed) == UNCONFIRMED
         *
         * - 추가사항: 반품상태가 SHIPPED 아닐 때에도 완료처리 가능한 예외상황이 있을 수 있다고 판단해 교한상태로 완료가능 판단 X
         * */

        // 1. 재고처리 완료가 가능한지 확인
        ReturningStockHistory returningStockHistory = returningStockHistoryRepository.findById(returningStockHistoryCode)
                .orElseThrow(() -> new IllegalArgumentException("타부서의 반품 별 재고 처리 완료 내역이 존재하지 않습니다."));
        if (returningStockHistory.getConfirmed() != Confirmed.UNCONFIRMED) {
            throw new IllegalArgumentException("반품 완료가 불가합니다. 이미 반품 완료된 내역입니다.");
        }

        // 2. 반품 별 재고 처리 완료 내역(tbl_returning_stock_history) 테이블 내역확인여부(confirmed) CONFIRMED 변경
        returningStockHistory = returningStockHistory.toBuilder()
                .confirmed(Confirmed.CONFIRMED)
                .build();
        returningStockHistoryRepository.save(returningStockHistory);

        // 3. 환불 처리도 완료되었다면, 전체 반품 처리
        // 3-1. 환불 처리도 완료되었는지 확인
        if (checkRefundComplete(returningStockHistory.getReturning())) {

            // 3-2. 전체 반품 처리
            completeAllReturning(returningStockHistory.getReturning());
        }
    }

    @Override
    @Transactional
    public void completeRefund(String loginId, int returningRefundHistoryCode) {
        /*TODO.
         * 환불만 완료인 경우 변하는 상태값
         * [1] 내역 확인 여부              - tbl_return_refund_history : confirmed = CONFIRMED
         * */

        /*
         * 환불 완료
         * - 환불 완료가 가능한 조건:
         *   1. 반품 별 환불 완료 내역(tbl_return_refund_history) 테이블 내역확인여부(confirmed) == UNCONFIRMED
         *
         * - 추가사항: 반품상태가 SHIPPED 아닐 때에도 완료처리 가능한 예외상황이 있을 수 있다고 판단해 교한상태로 완료가능 판단 X
         * */

        // 1. 환불 완료가 가능한지 확인
        ReturningRefundHistory returningRefundHistory = returningRefundHistoryRepository.findById(returningRefundHistoryCode)
                .orElseThrow(() -> new IllegalArgumentException("타부서의 반품 별 환불 처리 완료 내역이 존재하지 않습니다."));
        if (returningRefundHistory.getConfirmed() != Confirmed.UNCONFIRMED) {
            throw new IllegalArgumentException("환불 완료가 불가합니다. 이미 환불 완료된 내역입니다.");
        }

        // 2. 반품 별 환불 완료 내역(tbl_returning_refund_history) 테이블 내역확인여부(confirmed) CONFIRMED 변경
        returningRefundHistory = returningRefundHistory.toBuilder()
                .confirmed(Confirmed.CONFIRMED)
                .build();
        returningRefundHistoryRepository.save(returningRefundHistory);

        // 3. 재고 처리도 완료되었다면, 전체 반품 처리
        // 3-1. 재고 처리도 완료되었는지 확인
        if (checkStockComplete(returningRefundHistory.getReturning())) {

            // 3-2. 전체 반품 처리
            completeAllReturning(returningRefundHistory.getReturning());
        }
    }

    @Override
    @Transactional
    public void completeAllReturning(Returning returning) {
        /*TODO.
         * 반품 전체 완료(반품&환불)인 경우 변하는 상태값:
         * [1] 반품상태                   - tbl_return_status_history : status = COMPLETED (내역 추가됨)
         * [2] 반품완료된 상품의 재고        - tbl_stock : available_stock 추가 (가용재고 추가됨)
         * [3] 반품/교환요청 가능여부        - tbl_order_item : available = AVAILABLE -> XXXXX
         * */

        /*
         * 반품 전체 완료
         * - 반품 전체 완료가 가능한 조건:
         *   1. 반품 별 재고 처리 완료 내역(tbl_return_stock_history) 테이블 내역확인여부(confirmed) == CONFIRMED
         *   2. 반품 별 환불 완료 내역(tbl_return_refund_history) 테이블 내역확인여부(confirmed) == CONFIRMED
         *
         * - 추가사항: 반품상태가 SHIPPED 아닐 때에도 완료처리 가능한 예외상황이 있을 수 있다고 판단해 교한상태로 완료가능 판단 X
         * */

        // 1. 반품 완료가 가능한지 확인
        ReturningStockHistory returningStockHistory = returningStockHistoryRepository.findByReturning(returning)
                .orElseThrow(() -> new IllegalArgumentException("타부서의 반품 별 재고 처리 완료 내역이 존재하지 않습니다."));
        ReturningRefundHistory returningRefundHistory = returningRefundHistoryRepository.findByReturning(returning)
                .orElseThrow(() -> new IllegalArgumentException("타부서의 반품 별 환불 완료 내역이 존재하지 않습니다."));
        if (returningStockHistory.getConfirmed() != Confirmed.CONFIRMED) {
            throw new IllegalArgumentException("반품 전체 완료가 불가합니다. 재고처리(반품)가 완료되지 않았습니다.");
        } else if (returningRefundHistory.getConfirmed() != Confirmed.CONFIRMED) {
            throw new IllegalArgumentException("반품 전체 완료가 불가합니다. 환불이 완료되지 않았습니다.");
        }

        // 2. 반품 상태 이력(tbl_return_status_history) 테이블 반품상태에 반품상태(status) COMPLETED 내역 추가
        saveReturningStatusHistory(ReturningStatus.COMPLETED, returningStockHistory.getReturning());


        // 3. 재고(tbl_stock) 테이블 가용재고(available_stock) 증가

        // 3-1. 반품 별 재고 처리 완료 내역(tbl_return_stock_history) 테이블의 반품완료내역코드(return_stock_history_code)로
        //      반품 완료 상품 상태(tbl_return_item_status) 조회
        List<ReturningItemStatus> returningItemStatusList = returningItemStatusRepository
                .findByReturningItemStatusCode_ReturningStockHistoryCode(returningStockHistory.getReturningStockHistoryCode());

        // 3-2. 반품완료상품상태(tbl_return_item_status) 테이블의 상품코드(item_code)로 재고(tbl_stock) 조회
        //      창고코드(storage_code)=1인 재고 -> 임시로 창고코드 1인 재고만 조회
        for (ReturningItemStatus returningItemStatus : returningItemStatusList) {
            Stock stock = stockRepository.findByStorageCodeAndItemCode(1, returningItemStatus.getReturningItemStatusCode().getItemCode());

            // 4-4. 반품완료상품상태(tbl_return_item_status) 테이블의 재입고수량(restock_quantity) 만큼 재고(tbl_stock) 테이블의 가용재고(available_stock) 증가
            stock = stock.toBuilder()
                    .availableStock(stock.getAvailableStock() + returningItemStatus.getRestock_quantity())
                    .build();
            stockRepository.save(stock);
        }


//        // 4. 주문 별 상품(tbl_order_item) 테이블 반품/교환요청 가능여부(available) AVAILABLE 변경
//        List<ExOrderItem> exOrderItemList = exOrderItemRepository.findByOrderItemCode_orderCode(returningStockHistory.getReturning().getOrder().getOrderCode())
//                .orElseThrow(() -> new OrderNotFound("완료 처리와 연관된 주문 별 상품 내역을 찾을 수 없습니다."));
//        for (
//                ExOrderItem exOrderItem : exOrderItemList) {
//            exOrderItem = exOrderItem.toBuilder()
//                    .available(Available.AVAILABLE)
//                    .build();
//            exOrderItemRepository.save(exOrderItem);
//        }
    }

    @Override
    @Transactional
    public void drafterApproveReturning(ReturningDrafterApproveReqVO returningApproveReqVO, Returning returning, Member member) {
        // [1] 반품(tbl_return) 테이블 '기안자의 반품 승인 여부(drafter_approved)'가 APPROVE 라면
        //      -> 반품(tbl_return) 테이블 '반품 결재 상태(approval_status)' = UNCONFIRMED (변화 X)
        //      -> 반품 상태 이력(tbl_return_status_history) 테이블 '반품 상태(status)' = PENDING (내역 추가됨)
        //      -> 반품 별 결재자들(tbl_return_approver) 테이블 '승인여부(approved)' = APPROVED(기안자) / UNCONFIRMED(그 외 결재자)

        returning = returning.toBuilder()
                .drafterApproved(DrafterApproved.APPROVE)               // [1] 기안자의 반품 승인 여부
                .approvalStatus(Approval.UNCONFIRMED)                   // [2] 반품 결재 상태 (CANCELED 인 경우 UNCONFIRMED로 변경)
                .memberCode(member)                                     // 기안자 등록
                .comment(returningApproveReqVO.getComment())            // 첨언 등록
                .build();
        returningRepository.save(returning);

        // 반품 상태 이력(tbl_return_status_history) 테이블 '반품 상태(status)' = PENDING (내역 추가됨)
        saveReturningStatusHistory(ReturningStatus.PENDING, returning);

        // 반품 별 결재자들(tbl_return_approver) 테이블 '승인여부(approved)' = APPROVED (기안자만 결재자로 등록됨)
//        saveReturningApprover(member.getMemberCode(), returning, Approval.APPROVED, String.valueOf(LocalDateTime.now()), returning.getComment());

        // 3-2. 결재자들 등록
        for (Integer approverCode : returningApproveReqVO.getApproverCodeList()) {
            saveReturningApprover(approverCode, returning, Approval.UNCONFIRMED, null, null);
        }
    }

    @Override
    @Transactional
    public void drafterRejectReturning(ReturningDrafterApproveReqVO returningApproveReqVO, Returning returning, Member member) {
        // [1] 반품(tbl_return) 테이블 '기안자의 반품 승인 여부(drafter_approved)'가 REJECT 라면
        //      -> [2] 반품(tbl_return) 테이블 '반품 결재 상태(approval_status)' = REJECTED
        //      -> [3] 반품 상태 이력(tbl_return_status_history) 테이블 '반품 상태(status)' = REJECTED (내역 추가됨)
        //      -> [4] 반품 별 결재자들(tbl_return_approver) 테이블 '승인여부(approved)' = REJECTED (기안자만 결재자로 등록됨)

        returning = returning.toBuilder()
                .drafterApproved(DrafterApproved.REJECT)                // [1] 기안자의 반품 승인 여부
                .approvalStatus(Approval.REJECTED)                      // [2] 반품 결재 상태
                .memberCode(member)                                     // 기안자 등록
                .comment(returningApproveReqVO.getComment())             // 첨언 등록
                .build();
        returningRepository.save(returning);

        // 반품 상태 이력(tbl_return_status_history) 테이블 '반품 상태(status)' = REJECTED (내역 추가됨)
        saveReturningStatusHistory(ReturningStatus.REJECTED, returning);

        // 반품 별 결재자들(tbl_return_approver) 테이블 '승인여부(approved)' = REJECTED (기안자만 결재자로 등록됨)
//        saveReturningApprover(member.getMemberCode(), returning, Approval.REJECTED, String.valueOf(LocalDateTime.now()), returning.getComment());
    }

    @Override
    @Transactional
    public void saveReturningApprover(Integer member, Returning returning, Approval approval, String createdAt, String comment) {
        // 복합키 설정
        ReturningApproverCode drafterApproverCode = ReturningApproverCode.builder()
                .memberCode(member)                         // 멤버 코드 설정
                .returningCode(returning.getReturningCode())   // 반품 코드 설정
                .build();

        // 반품 별 결재자들 객체 생성
        ReturningApprover approver = ReturningApprover.builder()
                .returningApproverCode(drafterApproverCode)  // 복합키 설정
                .approved(approval)                         // [4] 반품 별 결재자들 (기안자)
                .createdAt(createdAt)
                .comment(comment)
                .active(true)
                .build();
        returningApproverRepository.save(approver);
    }

    @Override
    @Transactional
    public void saveReturningStatusHistory(ReturningStatus status, Returning returning) {
        ReturningStatusHistory returnStatusHistory = ReturningStatusHistory.builder()
                .status(status)   // [3] 반품 상태 이력
                .createdAt(String.valueOf(LocalDateTime.now()))
                .active(true)
                .returning(returning)
                .build();
        returningStatusHistoryRepository.save(returnStatusHistory);
    }

    @Override
    @Transactional
    public boolean checkRefundComplete(Returning returning) {
        if (returningRefundHistoryRepository.findByReturning(returning)
                .orElseThrow(() -> new IllegalArgumentException("반품 별 환불 완료 내역을 찾을 수 없습니다."))
                .getConfirmed() == Confirmed.CONFIRMED) return true;
        else return false;
    }

    @Override
    @Transactional
    public boolean checkStockComplete(Returning returning) {
        if (returningStockHistoryRepository.findByReturning(returning)
                .orElseThrow(() -> new IllegalArgumentException("반품 별 재고 처리 완료 내역을 찾을 수 없습니다."))
                .getConfirmed() == Confirmed.CONFIRMED) return true;
        else return false;
    }

}
