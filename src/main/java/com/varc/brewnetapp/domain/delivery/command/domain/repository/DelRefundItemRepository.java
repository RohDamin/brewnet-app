package com.varc.brewnetapp.domain.delivery.command.domain.repository;

import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.DelRefundItemPK;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelRefund;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelRefundItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DelRefundItemRepository extends JpaRepository<DelRefundItem, DelRefundItemPK> {

}
