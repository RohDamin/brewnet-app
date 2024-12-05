package com.varc.brewnetapp.domain.item.query.service;

import com.varc.brewnetapp.domain.item.query.dto.ItemDTO;
import com.varc.brewnetapp.domain.item.query.dto.MustBuyItemDTO;
import com.varc.brewnetapp.domain.item.query.mapper.ItemMapper;
import com.varc.brewnetapp.domain.item.query.mapper.MandatoryPurchaseMapper;
import com.varc.brewnetapp.domain.member.query.dto.MemberDTO;
import com.varc.brewnetapp.exception.EmptyDataException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;

import com.varc.brewnetapp.utility.time.Formatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service(value = "queryItemService")
public class ItemServiceImpl implements ItemService {
    private final ItemMapper itemMapper;
    private final MandatoryPurchaseMapper mandatoryPurchaseMapper;

    @Autowired
    public ItemServiceImpl(
            ItemMapper itemMapper,
            MandatoryPurchaseMapper mandatoryPurchaseMapper
    ) {
        this.itemMapper = itemMapper;
        this.mandatoryPurchaseMapper = mandatoryPurchaseMapper;
    }

    @Override
    @Transactional
    public Page<ItemDTO> findItemList(Pageable page, String itemName, String itemCode, String sort, String categoryCode, String correspondentCode) {
        long pageSize = page.getPageSize();
        long pageNumber = page.getPageNumber();
        long offset = pageNumber * pageSize;


        List<ItemDTO> itemList = itemMapper.selectItemList(offset, pageSize, itemName, itemCode, sort, categoryCode, correspondentCode);

        if (itemList.isEmpty() || itemList.size() < 0)
            throw new EmptyDataException("조회하려는 상품 정보가 없습니다");

        // 전체 데이터 개수 조회
        int count = itemMapper.selectItemListCnt(itemName, itemCode, categoryCode, correspondentCode);

        // PageImpl 객체로 감싸서 반환
        return new PageImpl<>(itemList, page, count);
    }

    @Override
    @Transactional
    public int findItemSellingPriceByItemCode(int itemCode) {
        return itemMapper.findItemPriceById(itemCode);
    }

    @Override
    @Transactional
    public Page<ItemDTO> findHqItemList(Pageable page, String itemName, String itemCode,
        String sort, String categoryCode, String correspondentCode) {
        long pageSize = page.getPageSize();
        long pageNumber = page.getPageNumber();
        long offset = pageNumber * pageSize;


        List<ItemDTO> itemList = itemMapper.selectHqItemList(offset, pageSize, itemName, itemCode, sort, categoryCode, correspondentCode);

        if (itemList.isEmpty() || itemList.size() < 0)
            throw new EmptyDataException("조회하려는 상품 정보가 없습니다");

        // 전체 데이터 개수 조회
        int count = itemMapper.selectHqItemListCnt(itemName, itemCode, categoryCode, correspondentCode);

        // PageImpl 객체로 감싸서 반환
        return new PageImpl<>(itemList, page, count);
    }

    @Override
    @Transactional
    public List<MustBuyItemDTO> getMustBuyItemsForHQ() {
        return mandatoryPurchaseMapper.getMandatoryPurchaseListForHq();
    }

    @Override
    @Transactional
    public List<MustBuyItemDTO> getMustBuyItemsForFranchise() {
        log.debug("getMustBuyItemsForFranchise called");
        return mandatoryPurchaseMapper.getMandatoryPurchaseListForFranchise(LocalDateTime.now());
    }

    @Override
    @Transactional
    public ItemDTO findItem(int itemCode) {

        ItemDTO item = itemMapper.selectItem(itemCode);

        if(item == null)
            throw new EmptyDataException("상품 정보가 없습니다");
        return item;
    }
}
