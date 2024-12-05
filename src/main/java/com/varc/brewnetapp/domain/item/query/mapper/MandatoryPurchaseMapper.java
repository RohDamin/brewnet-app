package com.varc.brewnetapp.domain.item.query.mapper;

import com.varc.brewnetapp.domain.item.query.dto.MustBuyItemDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MandatoryPurchaseMapper {
    List<MustBuyItemDTO> getMandatoryPurchaseListForHq();
    List<MustBuyItemDTO> getMandatoryPurchaseListForFranchise(
            @Param("currentTime") LocalDateTime currentTime
    );
}
