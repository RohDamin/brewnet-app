package com.varc.brewnetapp.domain.exchange.query.service;

import com.varc.brewnetapp.domain.exchange.enums.ExchangeStatus;
import com.varc.brewnetapp.domain.exchange.query.aggregate.vo.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ExchangeService {
    Page<ExchangeListVO> findExchangeList(Pageable page);

    Page<ExchangeListVO> searchExchangeList(String searchFilter, String searchWord, String startDate, String endDate, Pageable page);

    ExchangeDetailVO findExchangeDetailBy(Integer exchangeCode);

    Page<ExchangeHistoryVO> findExchangeHistoryList(Pageable page);

    Page<ExchangeHistoryVO> searchExchangeHistoryList(String searchFilter, String searchWord, String startDate, String endDate, Pageable page);

    ExchangeHistoryDetailVO findExchangeHistoryDetailBy(Integer exchangeStockHistoryCode);

    Page<FranExchangeListVO> findFranExchangeList(String loginId, Pageable page);

    Page<FranExchangeListVO> searchFranExchangeList(String loginId, String searchFilter, String searchWord, String startDate, String endDate, Pageable page);

    FranExchangeDetailVO findFranExchangeDetailBy(String loginId, int exchangeCode);

    List<FranExchangeStatusVO> findFranExchangeStatusBy(String loginId, int exchangeCode);

    ExchangeStatus findExchangeLatestStatus(int exchangeCode);

    List<ExchangeApproverVO> findExchangeApprover(String loginId, int exchangeCode);

    boolean isValidExchangeByFranchise(String loginId, int exchangeCode);

    boolean isValidOrderByFranchise(String loginId, int orderCode);
}