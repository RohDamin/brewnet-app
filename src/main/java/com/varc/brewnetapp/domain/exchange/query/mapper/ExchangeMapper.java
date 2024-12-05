package com.varc.brewnetapp.domain.exchange.query.mapper;

import com.varc.brewnetapp.common.domain.exchange.ExchangeStatus;
import com.varc.brewnetapp.domain.exchange.query.aggregate.vo.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ExchangeMapper {
    int selectExchangeListCnt();

    List<ExchangeListVO> selectExchangeList(long offset, long pageSize);

    List<ExchangeListVO> selectExcelExchangeList(String searchFilter, String searchWord, String startDate, String endDate);

    int selectRequestedExchangeListCnt();

    List<ExchangeListVO> selectRequestedExchangeList(long offset, long pageSize);

//    int selectSearchExchangeHistoryListCnt(String searchFilter, String searchWord, String startDate, String endDate);

    List<ExchangeListVO> selectSearchExchangeList(String searchFilter, String searchWord, String startDate, String endDate, long offset, long pageSize);

    ExchangeDetailVO selectExchangeDetailBy(int exchangeCode);

    int selectExchangeHistoryListCnt(String searchFilter, String searchWord, String startDate, String endDate);

    List<ExchangeHistoryVO> selectExchangeHistoryList(String searchFilter, String searchWord, String startDate, String endDate, long offset, long pageSize);

//    List<ExchangeHistoryVO> selectSearchExchangeHistoryList(String searchFilter, String searchWord, String startDate, String endDate, long offset, long pageSize);

    ExchangeHistoryDetailVO selectExchangeHistoryDetailBy(int exchangeStockHistoryCode);

    int selectFranExchangeListCnt(String loginId);

    List<FranExchangeListVO> selectFranExchangeList(String loginId, long offset, long pageSize);

    int selectSearchFranExchangeListCnt(String loginId, String searchFilter, String searchWord, String startDate, String endDate);

    List<FranExchangeListVO> selectSearchFranExchangeList(String loginId, String searchFilter, String searchWord, String startDate, String endDate, long offset, long pageSize);

    FranExchangeDetailVO selectFranExchangeDetailBy(int exchangeCode);

    List<FranExchangeStatusVO> selectFranExchangeStatusBy(int exchangeCode);

    Optional<ExchangeStatus> selectExchangeLatestStatusBy(int exchangeCode);

    List<ExchangeApproverVO> selectExchangeApproverBy(int exchangeCode);

    boolean selectValidExchangeByFranchise(String loginId, int exchangeCode);

    boolean selectValidOrderByFranchise(String loginId, int orderCode);

    List<Integer> selectAvailableExchangeBy(String loginId);

    List<FranExchangeItemVO> selectAvailableExchangeItemBy(int orderCode);

    int selectSearchExchangeListCnt(String searchFilter, String searchWord, String startDate, String endDate);
}
