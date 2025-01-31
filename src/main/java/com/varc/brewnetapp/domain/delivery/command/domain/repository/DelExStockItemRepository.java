package com.varc.brewnetapp.domain.delivery.command.domain.repository;

import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.DelExStockItemPK;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelExStockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DelExStockItemRepository extends JpaRepository<DelExStockItem, DelExStockItemPK> {

}
