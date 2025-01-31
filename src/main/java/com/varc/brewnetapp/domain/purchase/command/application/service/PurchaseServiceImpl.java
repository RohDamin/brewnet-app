package com.varc.brewnetapp.domain.purchase.command.application.service;

import com.varc.brewnetapp.domain.correspondent.command.domain.aggregate.Correspondent;
import com.varc.brewnetapp.domain.correspondent.command.domain.repository.CorrespondentItemRepository;
import com.varc.brewnetapp.domain.correspondent.command.domain.repository.CorrespondentRepository;
import com.varc.brewnetapp.domain.member.command.domain.aggregate.entity.Member;
import com.varc.brewnetapp.domain.member.command.domain.repository.MemberRepository;
import com.varc.brewnetapp.domain.member.command.domain.repository.PositionRepository;
import com.varc.brewnetapp.domain.purchase.command.application.dto.*;
import com.varc.brewnetapp.domain.purchase.command.domain.aggregate.*;
import com.varc.brewnetapp.domain.purchase.command.domain.repository.*;
import com.varc.brewnetapp.domain.purchase.common.IsApproved;
import com.varc.brewnetapp.domain.purchase.common.Status;
import com.varc.brewnetapp.domain.storage.command.domain.aggregate.Stock;
import com.varc.brewnetapp.domain.storage.command.domain.aggregate.Storage;
import com.varc.brewnetapp.domain.storage.command.domain.repository.StockRepository;
import com.varc.brewnetapp.domain.storage.command.domain.repository.StorageRepository;
import com.varc.brewnetapp.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service("PurchaseServiceCommand")
public class PurchaseServiceImpl implements PurchaseService {

    private final ModelMapper modelMapper;
    private final LetterOfPurchaseRepository letterOfPurchaseRepository;
    private final PurchaseStatusHistoryRepository purchaseStatusHistoryRepository;
    private final StockRepository stockRepository;
    private final CorrespondentRepository correspondentRepository;
    private final StorageRepository storageRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final CorrespondentItemRepository correspondentItemRepository;
    private final LetterOfPurchaseItemRepository letterOfPurchaseItemRepository;
    private final LetterOfPurchaseApproverRepository letterOfPurchaseApproverRepository;
    private final PurchaseSealRepository purchaseSealRepository;
    private final PurchasePrintRepository purchasePrintRepository;
    private final CompanyTempRepository companyTempRepository;
    private final MemberRepository memberRepository;
    private final PurchasePositionRepository purchasePositionRepository;

    @Autowired
    public PurchaseServiceImpl(ModelMapper modelMapper,
                               LetterOfPurchaseRepository letterOfPurchaseRepository,
                               PurchaseStatusHistoryRepository purchaseStatusHistoryRepository,
                               StockRepository stockRepository,
                               CorrespondentRepository correspondentRepository,
                               StorageRepository storageRepository,
                               PurchaseItemRepository purchaseItemRepository,
                               CorrespondentItemRepository correspondentItemRepository,
                               LetterOfPurchaseItemRepository letterOfPurchaseItemRepository,
                               LetterOfPurchaseApproverRepository letterOfPurchaseApproverRepository,
                               PurchaseSealRepository purchaseSealRepository,
                               PurchasePrintRepository purchasePrintRepository,
                               CompanyTempRepository companyTempRepository,
                               MemberRepository memberRepository, PositionRepository positionRepository, PurchasePositionRepository purchasePositionRepository) {
        this.modelMapper = modelMapper;
        this.letterOfPurchaseRepository = letterOfPurchaseRepository;
        this.purchaseStatusHistoryRepository = purchaseStatusHistoryRepository;
        this.stockRepository = stockRepository;
        this.correspondentRepository = correspondentRepository;
        this.storageRepository = storageRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.correspondentItemRepository = correspondentItemRepository;
        this.letterOfPurchaseItemRepository = letterOfPurchaseItemRepository;
        this.letterOfPurchaseApproverRepository = letterOfPurchaseApproverRepository;
        this.purchaseSealRepository = purchaseSealRepository;
        this.purchasePrintRepository = purchasePrintRepository;
        this.companyTempRepository = companyTempRepository;
        this.memberRepository = memberRepository;
        this.purchasePositionRepository = purchasePositionRepository;
    }

    @Transactional
    @Override
    public Integer createLetterOfPurchase(String loginId, PurchaseRequestDTO newPurchase) {

        LetterOfPurchase letterOfPurchase = modelMapper.map(newPurchase, LetterOfPurchase.class);

        Member member = memberRepository.findById(loginId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));

        Correspondent correspondent = correspondentRepository.findById(newPurchase.getCorrespondentCode())
                .orElseThrow(() -> new CorrespondentNotFoundException("존재하지 않는 거래처입니다."));

        Storage storage = storageRepository.findById(newPurchase.getStorageCode())
                .orElseThrow(() -> new StorageNotFoundException("존재하지 않는 창고입니다."));

        letterOfPurchase.setCreatedAt(LocalDateTime.now());
        letterOfPurchase.setActive(true);
        letterOfPurchase.setApproved(IsApproved.UNCONFIRMED);
        letterOfPurchase.setMember(member);
        letterOfPurchase.setCorrespondent(correspondent);
        letterOfPurchase.setStorage(storage);
        letterOfPurchase.setComment(newPurchase.getComment());
        letterOfPurchase.setSumPrice(0);

        // 구매품의서의 총 발주금액을 0으로 하여 일단 저장
        LetterOfPurchase savedPurchase = letterOfPurchaseRepository.save(letterOfPurchase);
        int totalPrice = 0;
        List<PurchaseItemDTO> items = newPurchase.getItems();

        for (PurchaseItemDTO purchaseItem : items) {
            PurchaseItem item = purchaseItemRepository.findById(purchaseItem.getItemCode())
                    .orElseThrow(() -> new ItemNotFoundException("존재하지 않는 품목입니다."));

            // 품목별 총 공급가액 누적 합산
            totalPrice += (item.getPurchasePrice() * purchaseItem.getQuantity());

            LetterOfPurchaseItem letterOfPurchaseItem = new LetterOfPurchaseItem(item.getItemCode(),
                                                                                savedPurchase.getLetterOfPurchaseCode(),
                                                                                purchaseItem.getQuantity(),
                                                                                false);

            // 구매품의서별 상품 저장
            letterOfPurchaseItemRepository.save(letterOfPurchaseItem);
        }

        // 총 발주금액 저장
        savedPurchase.setSumPrice(totalPrice);

        // 해당 구매품의서의 결재자 설정
        Member approver = memberRepository.findById(newPurchase.getApproverCode())
                                    .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));
        LetterOfPurchaseApprover purchaseApprover = new LetterOfPurchaseApprover();
        purchaseApprover.setMemberCode(approver.getMemberCode());
        purchaseApprover.setLetterOfPurchaseCode(savedPurchase.getLetterOfPurchaseCode());
        purchaseApprover.setApproved(IsApproved.UNCONFIRMED);
        purchaseApprover.setApprovedAt(null);
        purchaseApprover.setActive(true);
        purchaseApprover.setComment(null);
        letterOfPurchaseApproverRepository.save(purchaseApprover);

        // 해당 구매품의서의 상태 이력에 결재 요청 상태 추가
        PurchaseStatusHistory purchaseStatusHistory = new PurchaseStatusHistory();
        purchaseStatusHistory.setStatus(Status.REQUESTED);
        purchaseStatusHistory.setCreatedAt(LocalDateTime.now());
        purchaseStatusHistory.setActive(true);
        purchaseStatusHistory.setLetterOfPurchase(savedPurchase);
        purchaseStatusHistoryRepository.save(purchaseStatusHistory);

        // 새로 등록된 구매품의서의 구매품의서 코드 반환
        return savedPurchase.getLetterOfPurchaseCode();
    }

    @Transactional
    @Override
    public void cancelLetterOfPurchase(String loginId, PurchaseCancelRequestDTO cancelRequest) {

        LetterOfPurchase purchase = letterOfPurchaseRepository.findById(cancelRequest.getLetterOfPurchaseCode())
                                    .orElseThrow(() -> new PurchaseNotFoundException("존재하지 않는 구매품의서입니다."));

        Member member = memberRepository.findById(loginId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));

        // 로그인한 사용자가 기안자가 아니면 결재 요청 취소 불가
        if (!member.getMemberCode().equals(purchase.getMember().getMemberCode()))
            throw new AccessDeniedException("기안자가 아니면 결재 요청을 취소할 수 없습니다.");

        // 결재 처리 전까지만 결재 요청 취소 가능
        if (!purchase.getApproved().equals(IsApproved.UNCONFIRMED)) {
            throw new InvalidDataException("이미 결재 처리가 완료된 구매품의서입니다.");
        }

        // 해당 구매품의서 비활성화 처리
        purchase.setActive(false);

        // 해당 구매품의서의 상태 이력에 결재 취소 상태 추가
        PurchaseStatusHistory history = new PurchaseStatusHistory();
        history.setLetterOfPurchase(purchase);
        history.setStatus(Status.CANCELED);
        history.setCreatedAt(LocalDateTime.now());
        history.setActive(true);
        purchaseStatusHistoryRepository.save(history);
    }

    @Transactional
    @Override
    public void approveLetterOfPurchase(String loginId, int letterOfPurchaseCode, PurchaseApprovalRequestDTO request) {

        LetterOfPurchase requestedPurchase = letterOfPurchaseRepository.findById(letterOfPurchaseCode)
                                    .orElseThrow(() -> new PurchaseNotFoundException("존재하지 않는 구매품의서입니다."));

        LetterOfPurchaseApprover approver = letterOfPurchaseApproverRepository
                                            .findByLetterOfPurchaseCode(letterOfPurchaseCode);

        Member member = memberRepository.findById(loginId)
                .orElseThrow(()-> new MemberNotFoundException("존재하지 않는 회원입니다."));

        // 결재자가 맞는지 체크
        if (!member.getMemberCode().equals(approver.getMemberCode()))
            throw new InvalidDataException("해당 구매품의서의 결재자가 아닙니다.");

        // 아직 결재 전인 내역이 맞는지 체크
        if (!requestedPurchase.getApproved().equals(IsApproved.UNCONFIRMED))
            throw new InvalidDataException("이미 결재 처리가 완료된 구매품의서입니다.");

        // 결재 관련 정보 업데이트
        approver.setApproved(IsApproved.APPROVED);
        approver.setApprovedAt(LocalDateTime.now());
        approver.setComment(request.getComment());
        requestedPurchase.setApproved(IsApproved.APPROVED);

        Storage storage = requestedPurchase.getStorage();
        List<LetterOfPurchaseItem> items = letterOfPurchaseItemRepository
                                            .findByLetterOfPurchaseCode(letterOfPurchaseCode);

        // 발주 품목의 입고예정재고가 발주 수량만큼 증가
        for (LetterOfPurchaseItem item : items) {
            Stock stock = stockRepository.findByStorageCodeAndItemCode(storage.getStorageCode(), item.getItemCode());

            // 창고 생성 이후에 새로 등록된 상품이면 창고별 재고에 추가
            if (stock == null) {
                stock = new Stock(storage.getStorageCode(), item.getItemCode(),
                                    0, 0, item.getQuantity(), LocalDateTime.now(), true);
                stockRepository.save(stock);
                continue;
            }

            int inStock = stock.getInStock() + item.getQuantity();
            stock.setInStock(inStock);
        }

        // 해당 구매품의서의 상태 이력에 결재 승인 상태 추가
        PurchaseStatusHistory history = new PurchaseStatusHistory();
        history.setLetterOfPurchase(requestedPurchase);
        history.setStatus(Status.APPROVED);
        history.setCreatedAt(LocalDateTime.now());
        history.setActive(true);
        purchaseStatusHistoryRepository.save(history);
    }

    @Transactional
    @Override
    public void rejectLetterOfPurchase(String loginId, int letterOfPurchaseCode, PurchaseApprovalRequestDTO request) {

        LetterOfPurchase requestedPurchase = letterOfPurchaseRepository.findById(letterOfPurchaseCode)
                                    .orElseThrow(() -> new PurchaseNotFoundException("존재하지 않는 구매품의서입니다."));

        LetterOfPurchaseApprover approver = letterOfPurchaseApproverRepository
                                            .findByLetterOfPurchaseCode(letterOfPurchaseCode);

        Member member = memberRepository.findById(loginId)
                .orElseThrow(()-> new MemberNotFoundException("존재하지 않는 회원입니다."));

        // 결재자가 맞는지 체크
        if (!member.getMemberCode().equals(approver.getMemberCode()))
            throw new InvalidDataException("해당 구매품의서의 결재자가 아닙니다.");

        // 아직 결재 전인 내역이 맞는지 체크
        if (!requestedPurchase.getApproved().equals(IsApproved.UNCONFIRMED))
            throw new InvalidDataException("이미 결재 처리가 완료된 구매품의서입니다.");

        // 결재 관련 정보 업데이트
        approver.setApproved(IsApproved.REJECTED);
        approver.setApprovedAt(LocalDateTime.now());
        approver.setComment(request.getComment());
        requestedPurchase.setApproved(IsApproved.REJECTED);

        // 해당 구매품의서의 상태 이력에 결재 반려 상태 추가
        PurchaseStatusHistory history = new PurchaseStatusHistory();
        history.setLetterOfPurchase(requestedPurchase);
        history.setStatus(Status.REJECTED);
        history.setCreatedAt(LocalDateTime.now());
        history.setActive(true);
        purchaseStatusHistoryRepository.save(history);
    }

    @Transactional
    @Override
    public void changeInStockToAvailable(String loginId, ChangeInStockToAvailableRequestDTO bringIn) {

        LetterOfPurchase approvedPurchase = letterOfPurchaseRepository
                                            .findByLetterOfPurchaseCodeAndActiveTrue(bringIn.getLetterOfPurchaseCode());

        // 해당 구매품의서가 정상적으로 결재 승인된 상태인지 체크
        if (approvedPurchase == null) {
            throw new PurchaseNotFoundException("구매품의서가 삭제되었거나 존재하지 않습니다.");
        }
        else if (!approvedPurchase.getApproved().equals(IsApproved.APPROVED)) {
            throw new InvalidDataException("결재 승인되지 않은 구매품의서입니다.");
        }

        // 해당 구매품의서의 상품인지, 입고 처리 안 된 상품인지 체크
        LetterOfPurchaseItem purchaseItem = letterOfPurchaseItemRepository
                                            .findByLetterOfPurchaseCodeAndItemCode(bringIn.getLetterOfPurchaseCode(),
                                                                                    bringIn.getItemCode());

        if (purchaseItem == null) {
            throw new InvalidDataException("발주하지 않은 상품입니다.");
        } else if (purchaseItem.getStorageConfirmed()) {
            throw new InvalidDataException("이미 입고 처리된 상품입니다.");
        }

        Stock stock = stockRepository
                        .findByStorageCodeAndItemCode(approvedPurchase.getStorage().getStorageCode(),
                                                        purchaseItem.getItemCode());

        if (stock == null) throw new InvalidDataException("해당 창고에 없는 상품입니다.");

        // 구매품의서의 상품과 창고의 상품이 일치하는지 체크
        if (!stock.getItemCode().equals(purchaseItem.getItemCode()))
            throw new InvalidDataException("발주한 상품 코드와 창고의 상품 코드가 일치하지 않습니다.");

        // 입고예정재고는 발주 수량만큼 감소하고, 가용재고는 발주 수량만큼 증가
        int inStock = stock.getInStock() - purchaseItem.getQuantity();
        int availableStock = stock.getAvailableStock() + purchaseItem.getQuantity();

        stock.setInStock(inStock);
        stock.setAvailableStock(availableStock);

        // 발주 상품의 입고 처리 여부 true로 변경
        purchaseItem.setStorageConfirmed(true);
    }

    @Transactional
    @Override
    public void recordPurchasePrint(String loginId,
                                    int letterOfPurchaseCode,
                                    ExportPurchasePrintRequestDTO printRequest) {

        LetterOfPurchase letterOfPurchase = letterOfPurchaseRepository
                                            .findByLetterOfPurchaseCodeAndActiveTrue(letterOfPurchaseCode);

        // 발주서가 유효한건지, 결재 승인 처리된 건지 체크
        if (letterOfPurchase == null) {
            throw new PurchaseNotFoundException("발주서가 삭제되었거나 존재하지 않습니다.");
        }
        else if (!letterOfPurchase.getApproved().equals(IsApproved.APPROVED)) {
            throw new InvalidDataException("결재 승인되지 않은 발주서입니다.");
        }

        Member member = memberRepository.findById(loginId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));

        // 외부용 발주서 출력 내역 저장
        PurchasePrint purchasePrint = new PurchasePrint();
        purchasePrint.setReason(printRequest.getReason());
        purchasePrint.setPrintedAt(LocalDateTime.now());
        purchasePrint.setActive(true);
        purchasePrint.setMember(member);
        purchasePrint.setLetterOfPurchase(letterOfPurchase);
        purchasePrintRepository.save(purchasePrint);
    }

    @Transactional
    @Override
    public PurchasePrintResponseDTO exportPurchasePrint(int letterOfPurchaseCode) {

        LetterOfPurchase letterOfPurchase = letterOfPurchaseRepository
                .findByLetterOfPurchaseCodeAndActiveTrue(letterOfPurchaseCode);

        // 발주서가 유효한건지, 결재 승인 처리된 건지 체크
        if (letterOfPurchase == null) {
            throw new PurchaseNotFoundException("발주서가 삭제되었거나 존재하지 않습니다.");
        }
        else if (!letterOfPurchase.getApproved().equals(IsApproved.APPROVED)) {
            throw new InvalidDataException("결재 승인되지 않은 발주서입니다.");
        }

        // 외부용이므로 발주서에 법인 인감 코드 set
        PurchaseSeal seal = purchaseSealRepository.findTopByActiveTrueOrderBySealCodeDesc();
        letterOfPurchase.setSeal(seal);

        CompanyTemp company = companyTempRepository.findTopByActiveTrueOrderByCompanyCodeDesc();
        Correspondent correspondent = letterOfPurchase.getCorrespondent();
        Storage storage = letterOfPurchase.getStorage();

        PurchasePosition position = purchasePositionRepository.findById(letterOfPurchase.getMember().getPositionCode())
                .orElseThrow(() -> new PositionNotFoundException("존재하지 않는 직급입니다."));

        List<PurchasePrintItemDTO> printItems = new ArrayList<>();
        List<LetterOfPurchaseItem> purchaseItems = letterOfPurchaseItemRepository
                .findByLetterOfPurchaseCode(letterOfPurchaseCode);

        // 발주서의 상품 목록 불러오기
        for (LetterOfPurchaseItem purchaseItem : purchaseItems) {
            PurchaseItem item = purchaseItemRepository.findByItemCodeAndActiveTrue(purchaseItem.getItemCode());

            if (item == null) throw new ItemNotFoundException("삭제되었거나 존재하지 않는 상품입니다.");

            int vatPrice = (item.getPurchasePrice() / 10);
            PurchasePrintItemDTO printItem = new PurchasePrintItemDTO(item.getName(),
                                                                        item.getUniqueCode(),
                                                                        item.getPurchasePrice(),
                                                                        vatPrice,
                                                                        purchaseItem.getQuantity());

            printItems.add(printItem);
        }

        // 외부용 발주서 출력 응답 반환
        PurchasePrintResponseDTO printResponse = new PurchasePrintResponseDTO();
        printResponse.setLetterOfPurchaseCode(letterOfPurchaseCode);
        printResponse.setCreatedAt((letterOfPurchase.getCreatedAt())
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        printResponse.setMemberName(letterOfPurchase.getMember().getName());
        printResponse.setPositionName(position.getName());
        printResponse.setCompanyName(company.getName());
        printResponse.setBusinessNumber(company.getBusinessNumber());
        printResponse.setCorporateNumber(company.getCorporateNumber());
        printResponse.setCeoName(company.getCeoName());
        printResponse.setCompanyContact(company.getContact());
        printResponse.setSealImageUrl(seal.getImageUrl());
        printResponse.setItems(printItems);
        printResponse.setSumPrice(letterOfPurchase.getSumPrice());
        printResponse.setVatSum(letterOfPurchase.getSumPrice() / 10);
        printResponse.setCorrespondentName(correspondent.getName());
        printResponse.setManagerName(correspondent.getManagerName());
        printResponse.setCorrespondentContact(correspondent.getContact());
        printResponse.setStorageName(storage.getName());
        printResponse.setStorageAddress(storage.getAddress());
        printResponse.setStorageContact(storage.getContact());

        return printResponse;
    }

    @Transactional
    @Override
    public PurchasePrintResponseDTO takeInHousePurchasePrint(String loginId, int letterOfPurchaseCode) {

        LetterOfPurchase letterOfPurchase = letterOfPurchaseRepository
                                            .findByLetterOfPurchaseCodeAndActiveTrue(letterOfPurchaseCode);

        // 발주서가 유효한건지, 결재 승인 처리된 건지 체크
        if (letterOfPurchase == null) {
            throw new PurchaseNotFoundException("발주서가 삭제되었거나 존재하지 않습니다.");
        }
        else if (!letterOfPurchase.getApproved().equals(IsApproved.APPROVED)) {
            throw new InvalidDataException("결재 승인되지 않은 발주서입니다.");
        }

        // 존재하는 회원인지 체크
        Member member = memberRepository.findById(loginId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));

        // 내부용 발주서로 변경되어 연결된 법인 인감 코드 제거
        letterOfPurchase.setSeal(null);

        CompanyTemp company = companyTempRepository.findTopByActiveTrueOrderByCompanyCodeDesc();
        Correspondent correspondent = letterOfPurchase.getCorrespondent();
        Storage storage = letterOfPurchase.getStorage();

        PurchasePosition position = purchasePositionRepository.findById(letterOfPurchase.getMember().getPositionCode())
                .orElseThrow(() -> new PositionNotFoundException("존재하지 않는 직급입니다."));

        List<PurchasePrintItemDTO> printItems = new ArrayList<>();
        List<LetterOfPurchaseItem> purchaseItems = letterOfPurchaseItemRepository
                                                    .findByLetterOfPurchaseCode(letterOfPurchaseCode);

        // 발주서의 상품 목록 불러오기
        for (LetterOfPurchaseItem purchaseItem : purchaseItems) {
            PurchaseItem item = purchaseItemRepository.findByItemCodeAndActiveTrue(purchaseItem.getItemCode());

            if (item == null) throw new ItemNotFoundException("삭제되었거나 존재하지 않는 상품입니다.");

            int vatPrice = (item.getPurchasePrice() / 10);
            PurchasePrintItemDTO printItem = new PurchasePrintItemDTO(item.getName(),
                                                                        item.getUniqueCode(),
                                                                        item.getPurchasePrice(),
                                                                        vatPrice,
                                                                        purchaseItem.getQuantity());

            printItems.add(printItem);
        }

        // 내부용 발주서 출력 응답 반환
        PurchasePrintResponseDTO printResponse = new PurchasePrintResponseDTO();
        printResponse.setLetterOfPurchaseCode(letterOfPurchaseCode);
        printResponse.setCreatedAt((letterOfPurchase.getCreatedAt())
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        printResponse.setMemberName(letterOfPurchase.getMember().getName());
        printResponse.setPositionName(position.getName());
        printResponse.setCompanyName(company.getName());
        printResponse.setBusinessNumber(company.getBusinessNumber());
        printResponse.setCorporateNumber(company.getCorporateNumber());
        printResponse.setCeoName(company.getCeoName());
        printResponse.setCompanyContact(company.getContact());
        printResponse.setSealImageUrl(null);
        printResponse.setItems(printItems);
        printResponse.setSumPrice(letterOfPurchase.getSumPrice());
        printResponse.setVatSum(letterOfPurchase.getSumPrice() / 10);
        printResponse.setCorrespondentName(correspondent.getName());
        printResponse.setManagerName(correspondent.getManagerName());
        printResponse.setCorrespondentContact(correspondent.getContact());
        printResponse.setStorageName(storage.getName());
        printResponse.setStorageAddress(storage.getAddress());
        printResponse.setStorageContact(storage.getContact());

        return printResponse;
    }

    @Transactional
    @Override
    public void sendLetterOfPurchase(String loginId, int letterOfPurchaseCode) {

        LetterOfPurchase letterOfPurchase = letterOfPurchaseRepository
                                            .findByLetterOfPurchaseCodeAndActiveTrue(letterOfPurchaseCode);

        // 발주서가 유효한건지, 결재 승인 처리된 건지 체크
        if (letterOfPurchase == null) {
            throw new PurchaseNotFoundException("발주서가 삭제되었거나 존재하지 않습니다.");
        }
        else if (!letterOfPurchase.getApproved().equals(IsApproved.APPROVED)) {
            throw new InvalidDataException("결재 승인되지 않은 발주서입니다.");
        }
    }
}
