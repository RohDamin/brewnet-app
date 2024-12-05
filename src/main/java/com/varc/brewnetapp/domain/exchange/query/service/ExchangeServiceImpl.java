package com.varc.brewnetapp.domain.exchange.query.service;

import com.varc.brewnetapp.common.domain.exchange.ExchangeStatus;
import com.varc.brewnetapp.domain.exchange.query.aggregate.vo.*;
import com.varc.brewnetapp.domain.exchange.query.mapper.ExchangeMapper;
import com.varc.brewnetapp.exception.ExchangeNotFoundException;
import com.varc.brewnetapp.exception.UnauthorizedAccessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service("ExchangeServiceQuery")
@Slf4j
public class ExchangeServiceImpl implements ExchangeService {

    private final ExchangeMapper exchangeMapper;

    @Autowired
    public ExchangeServiceImpl(ExchangeMapper exchangeMapper) {
        this.exchangeMapper = exchangeMapper;
    }

    @Override
    public Page<ExchangeListVO> findExchangeList(Pageable page) {
        // 페이징 정보 추가
        long offset = page.getOffset();
        long pageSize = page.getPageSize();

        // DB에서 교환 목록 조회
        List<ExchangeListVO> exchangeList = exchangeMapper.selectExchangeList(offset, pageSize);

        // 전체 데이터 개수 조회
        int count = exchangeMapper.selectExchangeListCnt();

        // PageImpl 객체로 감싸서 반환
        return new PageImpl<>(exchangeList, page, count);
    }

    @Override
    public List<ExchangeListVO> findExcelExchangeList(String searchFilter, String searchWord, String startDate, String endDate) {
        List<ExchangeListVO> exchangeList = exchangeMapper.selectExcelExchangeList(searchFilter, searchWord, startDate, endDate);
        return exchangeList;
    }

    @Override
    public Page<ExchangeListVO> findRequestedExchangeList(Pageable page) {
        // 페이징 정보 추가
        long offset = page.getOffset();
        long pageSize = page.getPageSize();

        // DB에서 교환 목록 조회
        List<ExchangeListVO> exchangeList = exchangeMapper.selectRequestedExchangeList(offset, pageSize);

        // 전체 데이터 개수 조회
        int count = exchangeMapper.selectRequestedExchangeListCnt();

        // PageImpl 객체로 감싸서 반환
        return new PageImpl<>(exchangeList, page, count);
    }

    @Override
    public Page<ExchangeListVO> searchExchangeList(String searchFilter, String searchWord, String startDate, String endDate, Pageable page) {
        // 페이징 정보 추가
        long offset = page.getOffset();
        long pageSize = page.getPageSize();

        // DB에서 교환 목록 조회
        List<ExchangeListVO> exchangeList = exchangeMapper.selectSearchExchangeList(searchFilter, searchWord, startDate, endDate, offset, pageSize);


        // 전체 데이터 개수 조회
        int count = exchangeMapper.selectSearchExchangeListCnt(searchFilter, searchWord, startDate, endDate);

        // PageImpl 객체로 감싸서 반환
        return new PageImpl<>(exchangeList, page, count);
    }

    @Override
    public ExchangeDetailVO findExchangeDetailBy(Integer exchangeCode) {
        ExchangeDetailVO exchangeDetail = exchangeMapper.selectExchangeDetailBy(exchangeCode);
        return exchangeDetail;
    }

    @Override
    public Page<ExchangeHistoryVO> findExchangeHistoryList(String searchFilter, String searchWord, String startDate, String endDate, Pageable page) {
        // 페이징 정보 추가
        long offset = page.getOffset();
        long pageSize = page.getPageSize();

        // DB에서 교환 목록 조회
        List<ExchangeHistoryVO> exchangeHistoryList = exchangeMapper.selectExchangeHistoryList(searchFilter, searchWord, startDate, endDate, offset, pageSize);

        // 전체 데이터 개수 조회
        int count = exchangeMapper.selectExchangeHistoryListCnt(searchFilter, searchWord, startDate, endDate);

        // PageImpl 객체로 감싸서 반환
        return new PageImpl<>(exchangeHistoryList, page, count);
    }

//    @Override
//    public Page<ExchangeHistoryVO> searchExchangeHistoryList(String searchFilter, String searchWord, String startDate, String endDate, Pageable page) {
//        // 페이징 정보 추가
//        long offset = page.getOffset();
//        long pageSize = page.getPageSize();
//
//        // DB에서 교환 목록 조회
//        List<ExchangeHistoryVO> exchangeHistoryList = exchangeMapper.selectSearchExchangeHistoryList(searchFilter, searchWord, startDate, endDate, offset, pageSize);
//
//        // 전체 데이터 개수 조회
//        int count = exchangeMapper.selectSearchExchangeHistoryListCnt(searchFilter, searchWord, startDate, endDate);
//
//        // PageImpl 객체로 감싸서 반환
//        return new PageImpl<>(exchangeHistoryList, page, count);
//    }

    @Override
    public ExchangeHistoryDetailVO findExchangeHistoryDetailBy(Integer exchangeStockHistoryCode) {
        ExchangeHistoryDetailVO exchangeHistoryDetail = exchangeMapper.selectExchangeHistoryDetailBy(exchangeStockHistoryCode);
        return exchangeHistoryDetail;
    }

    @Override
    public Page<FranExchangeListVO> findFranExchangeList(String loginId, Pageable page) {
        // 페이징 정보 추가
        long offset = page.getOffset();
        long pageSize = page.getPageSize();

        // DB에서 교환 목록 조회
        List<FranExchangeListVO> franExchangeList = exchangeMapper.selectFranExchangeList(loginId, offset, pageSize);

        // 전체 데이터 개수 조회
        int count = exchangeMapper.selectFranExchangeListCnt(loginId);

        // PageImpl 객체로 감싸서 반환
        return new PageImpl<>(franExchangeList, page, count);
    }

    @Override
    public Page<FranExchangeListVO> searchFranExchangeList(String loginId, String searchFilter, String searchWord, String startDate, String endDate, Pageable page) {
        // 페이징 정보 추가
        long offset = page.getOffset();
        long pageSize = page.getPageSize();

        // DB에서 교환 목록 조회
        List<FranExchangeListVO> franExchangeList = exchangeMapper.selectSearchFranExchangeList(loginId, searchFilter, searchWord, startDate, endDate, offset, pageSize);

        // 전체 데이터 개수 조회
        int count = exchangeMapper.selectSearchFranExchangeListCnt(loginId, searchFilter, searchWord, startDate, endDate);

        // PageImpl 객체로 감싸서 반환
        return new PageImpl<>(franExchangeList, page, count);
    }

    @Override
    public FranExchangeDetailVO findFranExchangeDetailBy(String loginId, int exchangeCode) {
        // 해당 가맹점에서 교환신청한 내역이 맞는지 검증
        if (isValidExchangeByFranchise(loginId, exchangeCode)) {
            FranExchangeDetailVO franExchangeDetail = exchangeMapper.selectFranExchangeDetailBy(exchangeCode);
            return franExchangeDetail;
        } else {
            throw new UnauthorizedAccessException("로그인한 가맹점에서 작성한 교환 요청만 조회할 수 있습니다");
        }
    }

    @Override
    public List<FranExchangeStatusVO> findFranExchangeStatusBy(String loginId, int exchangeCode) {
        if (isValidExchangeByFranchise(loginId, exchangeCode)) {
            List<FranExchangeStatusVO> franExchangeStatus = exchangeMapper.selectFranExchangeStatusBy(exchangeCode);
            return franExchangeStatus;
        } else {
            throw new UnauthorizedAccessException("로그인한 가맹점에서 작성한 교환 요청 상태만 조회할 수 있습니다");
        }

    }

//    @Override
//    public Workbook exportExchangeExcel() {
//        Workbook workbook = new XSSFWorkbook();
//        Sheet sheet = workbook.createSheet(); //excel 파일 생성
//
//        List<String> headers = Arrays.asList("교환번호", "교환요청지점", "교환품목명", "교환사유",
//                "교환담당자", "교환요청일자", "교환상태", "교환 승인 상태");  // 헤더 데이터
//
//        List<ExchangeListVO> rows = exchangeMapper.selectAllExchangeList();
//        List<List<String>> rowData = new ArrayList<>();
//        for (ExchangeListVO exchange : rows) {
//            List<String> row = new ArrayList<>();
//            row.add(String.valueOf(exchange.getExchangeCode()));      // 교환번호
//            row.add(exchange.getFranchiseName());                     // 교환요청지점
//            row.add(exchange.getItemName());                          // 교환품목명
//            row.add(exchange.getReason() != null ? exchange.getReason().getKrName() : ""); // 교환사유 (null 체크)
//            row.add(exchange.getMemberCode());                        // 교환담당자
//            row.add(exchange.getCreatedAt());                         // 교환요청일자
//            row.add(exchange.getStatus() != null ? exchange.getStatus().getKrName() : ""); // 교환상태 (null 체크)
//            row.add(exchange.getApprovalStatus() != null ? exchange.getApprovalStatus().getKrName() : ""); // 교환 승인 상태 (null 체크)
//
//            rowData.add(row);
//        }
//
//        // 헤더 세팅
//        Row headerRow = sheet.createRow(0); //0번째 줄 생성 - 헤더(맨 윗줄)
//        for (int i = 0; i < headers.size(); i++) {
//            Cell cell = headerRow.createCell(i);
//            cell.setCellValue(headers.get(i));
//        }
//
//        // 데이터 세팅
//        for (int i = 0; i < rows.size(); i++) { // 0부터 시작
//            Row row = sheet.createRow(i + 1); // 데이터는 1번 줄부터 시작
//            List<String> data = rowData.get(i);
//            for (int j = 0; j < data.size(); j++) {
//                Cell cell = row.createCell(j);
//                cell.setCellValue(data.get(j));
//            }
//        }
//
//        return workbook;
//    }

    /* 교환코드로 가장 최근 교환상태(status) 1개를 조회하는 메서드 */
    // 교환취소 시, 해당 교환요청의 상태가 REQUESTED인지 조회하기 위해 사용 (컨트롤러 X)
    @Override
    public ExchangeStatus findExchangeLatestStatus(int exchangeCode) {
        ExchangeStatus latestStatus = exchangeMapper.selectExchangeLatestStatusBy(exchangeCode)
                .orElseThrow(() -> new ExchangeNotFoundException("교환 상태를 찾을 수 없습니다."));
        return latestStatus;
    }

    /* 교환코드로 결재상황 리스트를 조회하기 위해 사용하는 메서드 */
    // (본사)교환상세보기 페이지 - '결재진행상황' 버튼 클릭 시 사용
    @Override
    public List<ExchangeApproverVO> findExchangeApprover(int exchangeCode) {
        List<ExchangeApproverVO> exchangeApproverList = exchangeMapper.selectExchangeApproverBy(exchangeCode);
        return exchangeApproverList;
    }

    /* 유저 아이디(loginId)와 교환코드(exchangeCode)로 해당 가맹점의 주문이 맞는지 검증하는 메서드 */
    // 가맹점 목록조회/가맹점 상세조회에서 유효한 요청인지 검증하기 위해 사용
    @Override
    public boolean isValidExchangeByFranchise(String loginId, int exchangeCode) {
        return exchangeMapper.selectValidExchangeByFranchise(loginId, exchangeCode);
    }

    // fix: 이동 필요
    /* 유저 아이디(loginId)와 주문코드(orderCode)로 해당 가맹점의 주문이 맞는지 검증하는 메서드 */
    // 가맹점 교환신청 시 유효한 요청인지 검증하기 위해 사용
    @Override
    public boolean isValidOrderByFranchise(String loginId, int orderCode) {
        return exchangeMapper.selectValidOrderByFranchise(loginId, orderCode);
    }

    /* 유저 아이디(loginId)로 교환신청 가능한 주문코드 목록을 찾는 메서드 */
    // 가맹점 교환신청 시 교환신청 가능한 주문 목록 찾기 위해 사용(주문에 교환신청 가능한 물품이 1건 이상인 경우에 포함됨)
    @Override
    public List<Integer> findFranAvailableExchangeBy(String loginId) {
        return exchangeMapper.selectAvailableExchangeBy(loginId);
    }

    /* 주문코드(orderCode)로 교환신청 가능한 그 주문의 상품 리스트 찾는 메서드 */
    // 가맹점 교환신청 시 선택한 주문코드에서 교환신청 가능한 상품목록을 찾기 위해 사용
    @Override
    public List<FranExchangeItemVO> findFranAvailableExchangeItemBy(String loginId, int orderCode) {
        // 해당 가맹점에서 주문한 내역이 맞는지 검증
        if (isValidOrderByFranchise(loginId, orderCode)) {
            return exchangeMapper.selectAvailableExchangeItemBy(orderCode);
        } else {
            throw new UnauthorizedAccessException("로그인한 가맹점에서 신청한 주문의 상품 리스트만 조회할 수 있습니다");
        }
    }
}
