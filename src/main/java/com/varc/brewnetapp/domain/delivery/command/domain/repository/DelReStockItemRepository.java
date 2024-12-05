package com.varc.brewnetapp.domain.delivery.command.domain.repository;

import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.DelReStockItemPK;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelReStock;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelReStockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DelReStockItemRepository extends JpaRepository<DelReStockItem, DelReStockItemPK> {

}
