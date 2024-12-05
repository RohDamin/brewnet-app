package com.varc.brewnetapp.domain.item.query.service;

import com.varc.brewnetapp.domain.item.query.dto.ItemDTO;
import com.varc.brewnetapp.domain.item.query.dto.MustBuyItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ItemService {

    Page<ItemDTO> findItemList(Pageable page, String itemName, String itemCode, String sort, String categoryCode, String correspondentCode);
    int findItemSellingPriceByItemCode(int itemCode);

    Page<ItemDTO> findHqItemList(Pageable page, String itemName, String itemCode, String sort, String categoryCode, String correspondentCode);

    List<MustBuyItemDTO> getMustBuyItemsForHQ();
    List<MustBuyItemDTO> getMustBuyItemsForFranchise();

    ItemDTO findItem(int itemCode);
}
