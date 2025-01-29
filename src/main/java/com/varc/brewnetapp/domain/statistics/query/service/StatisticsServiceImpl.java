package com.varc.brewnetapp.domain.statistics.query.service;

import com.varc.brewnetapp.domain.member.command.domain.aggregate.entity.Member;
import com.varc.brewnetapp.domain.member.command.domain.repository.MemberRepository;
import com.varc.brewnetapp.domain.member.query.dto.ApprovalDTO;
import com.varc.brewnetapp.domain.statistics.query.dto.MyWaitApprovalDTO;
import com.varc.brewnetapp.domain.statistics.query.dto.OrderCountPriceDTO;
import com.varc.brewnetapp.domain.statistics.query.dto.OrderItemStatisticsDTO;
import com.varc.brewnetapp.domain.statistics.query.dto.OrderStatisticsDTO;
import com.varc.brewnetapp.domain.statistics.query.dto.SafeStockStatisticsDTO;
import com.varc.brewnetapp.domain.statistics.query.dto.newOrderDTO;
import com.varc.brewnetapp.domain.statistics.query.mapper.StatisticsMapper;
import com.varc.brewnetapp.exception.EmptyDataException;
import com.varc.brewnetapp.exception.InvalidDataException;
import com.varc.brewnetapp.exception.MemberNotFoundException;
import com.varc.brewnetapp.security.utility.JwtUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service(value = "queryStatisticsService")
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;
    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    @Autowired
    public StatisticsServiceImpl(StatisticsMapper statisticsMapper, JwtUtil jwtUtil,
        MemberRepository memberRepository) {
        this.statisticsMapper = statisticsMapper;
        this.jwtUtil = jwtUtil;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public OrderStatisticsDTO findOrderStatistics(String startDate, String endDate) {

        LocalDate newDate = (LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))).plusDays(1);

        endDate = newDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        OrderStatisticsDTO orderStatistics = statisticsMapper.selectOrderStatistics(startDate, endDate);
        List<OrderItemStatisticsDTO> orderItemStatisticsList = statisticsMapper.selectOrderItemStatistics(startDate, endDate);
        int orderItemStatisticsCount = statisticsMapper.selectOrderItemStatisticsCnt(startDate, endDate);

        int otherCount = orderItemStatisticsCount;
        double other = 0;

        if(orderItemStatisticsList != null && orderItemStatisticsList.size() > 0) {
            for(OrderItemStatisticsDTO orderItemStatistics : orderItemStatisticsList) {
                double percentage = roundItemPercent(((double) orderItemStatistics.getItemCount() / orderItemStatisticsCount) * 100);
                orderItemStatistics.setItemPercent(percentage);
                other += percentage;
                otherCount -= orderItemStatistics.getItemCount();
            }
        } else
            throw new EmptyDataException("주문 상품이 없어 통계를 제공할 수 없습니다");

        other = roundItemPercent(100 - other);

        OrderItemStatisticsDTO otherItem = new OrderItemStatisticsDTO("기타", other, otherCount);

        orderItemStatisticsList.add(otherItem);
        orderStatistics.setItems(orderItemStatisticsList);

        return orderStatistics;
    }

    @Override
    @Transactional
    public List<OrderCountPriceDTO> findOrderCountPriceStatistics(String yearMonth) {

        if (yearMonth == null)
            yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<OrderCountPriceDTO> orderCountPriceList = statisticsMapper.selectOrderCountPriceList(yearMonth);

        if(orderCountPriceList != null && orderCountPriceList.size() > 0)
            return orderCountPriceList;
        else
            throw new EmptyDataException("해당 년월에 해당하는 주문 건수와 가격 통계가 없습니다");

    }

    @Override
    @Transactional
    public Page<SafeStockStatisticsDTO> findSafeStock(Pageable page) {

        long pageSize = page.getPageSize();
        long pageNumber = page.getPageNumber();
        long offset = pageNumber * pageSize;
        int minPurchaseCount = 0;

        List<SafeStockStatisticsDTO> safeStockStatisticsList = statisticsMapper.selectSafeStock(offset, pageSize);

        if(safeStockStatisticsList != null && safeStockStatisticsList.size() > 0){

            for(SafeStockStatisticsDTO safeStockStatisticsDTO : safeStockStatisticsList){
                Integer unApprovedItemCount = statisticsMapper.selectUnApprovedItemCount(safeStockStatisticsDTO.getItemCode());

                safeStockStatisticsDTO.setUnApprovedOrderCount(unApprovedItemCount);

                if(safeStockStatisticsDTO.getAvailableMinusSafeStock() < 0)
                    minPurchaseCount = -(Math.abs(safeStockStatisticsDTO.getAvailableMinusSafeStock()) + unApprovedItemCount);
                else
                    minPurchaseCount = safeStockStatisticsDTO.getAvailableMinusSafeStock() - unApprovedItemCount;

                if(minPurchaseCount < 0)
                    safeStockStatisticsDTO.setMinPurchaseCount(minPurchaseCount);
                else
                    safeStockStatisticsDTO.setMinPurchaseCount(null);
            }

            int count = statisticsMapper.selectSafeStockCnt();
            return new PageImpl<>(safeStockStatisticsList, page, count);
        }

        else
            throw new EmptyDataException("안전 재고 위험 알림값이 없습니다");

    }

    @Override
    @Transactional
    public Page<newOrderDTO> findNewOrder(Pageable page) {
        long pageSize = page.getPageSize();
        long pageNumber = page.getPageNumber();
        long offset = pageNumber * pageSize;
        int count = 0;

        List<newOrderDTO> newOrderList = statisticsMapper.selectNewOrder(offset, pageSize);

        if(newOrderList != null && newOrderList.size() > 0)
            count = statisticsMapper.selectNewOrderCnt();
        else
            throw new EmptyDataException("신규 주문이 없습니다");

        return new PageImpl<>(newOrderList, page, count);
    }

    @Override
    @Transactional
    public Page<MyWaitApprovalDTO> findMyWaitApproval(Pageable page, String accessToken) {
        long pageSize = page.getPageSize();
        long pageNumber = page.getPageNumber();
        long offset = pageNumber * pageSize;
        int count = 0;

        String loginId = jwtUtil.getLoginId(accessToken.replace("Bearer ", ""));
        Member member = memberRepository.findById(loginId)
            .orElseThrow(() -> new MemberNotFoundException("조회하려는 멤버 정보가 없습니다"));

        List<MyWaitApprovalDTO> approvalList = statisticsMapper.selectApprovalList(pageSize, offset, member.getMemberCode());

        if(approvalList != null && approvalList.size() > 0)
            count = statisticsMapper.selectApprovalListCnt(member.getMemberCode());
        else
            throw new EmptyDataException("내 결재가 없습니다");

        return new PageImpl<>(approvalList, page, count);
    }

    public double roundItemPercent(double itemPercent) {
        BigDecimal bd = new BigDecimal(itemPercent).setScale(1, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
