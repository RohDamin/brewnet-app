package com.varc.brewnetapp.domain.delivery.command.application.service;


import com.varc.brewnetapp.common.domain.order.Available;
import com.varc.brewnetapp.domain.delivery.command.application.dto.CreateDeliveryStatusRequestDTO;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.DeliveryKind;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.DeliveryStatus;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelExStock;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelExStock.ConfirmationStatus;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelExStockItem;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelReStock;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelReStock.StockStatus;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelReStockItem;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelRefund;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelRefund.RefundStatus;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DelRefundItem;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DeliveryExchange;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DeliveryExchangeStatusHistory;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DeliveryExchangeStatusHistory.ExchangeStatus;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DeliveryOrder;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DeliveryOrderStatusHistory;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DeliveryReturn;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DeliveryReturnStatusHistory;
import com.varc.brewnetapp.domain.delivery.command.domain.aggregate.entity.DeliveryReturnStatusHistory.ReturnStatus;
import com.varc.brewnetapp.domain.delivery.command.domain.repository.DelExStockItemRepository;
import com.varc.brewnetapp.domain.delivery.command.domain.repository.DelExStockRepository;
import com.varc.brewnetapp.domain.delivery.command.domain.repository.DelReStockItemRepository;
import com.varc.brewnetapp.domain.delivery.command.domain.repository.DelReStockRepository;
import com.varc.brewnetapp.domain.delivery.command.domain.repository.DelRefundItemRepository;
import com.varc.brewnetapp.domain.delivery.command.domain.repository.DelRefundRepository;
import com.varc.brewnetapp.domain.delivery.command.domain.repository.DeliveryExchangeRepository;
import com.varc.brewnetapp.domain.delivery.command.domain.repository.DeliveryExchangeStatusHistoryRepository;
import com.varc.brewnetapp.domain.delivery.command.domain.repository.DeliveryOrderRepository;
import com.varc.brewnetapp.domain.delivery.command.domain.repository.DeliveryOrderStatusHistoryRepository;
import com.varc.brewnetapp.domain.delivery.command.domain.repository.DeliveryReturnRepository;
import com.varc.brewnetapp.domain.delivery.command.domain.repository.DeliveryReturnStatusHistoryRepository;
import com.varc.brewnetapp.domain.exchange.command.application.repository.ExchangeItemRepository;
import com.varc.brewnetapp.domain.exchange.command.application.repository.ExchangeRepository;
import com.varc.brewnetapp.domain.exchange.command.domain.aggregate.entity.ExchangeItem;
import com.varc.brewnetapp.domain.member.command.domain.aggregate.entity.Member;
import com.varc.brewnetapp.domain.member.command.domain.repository.MemberRepository;
import com.varc.brewnetapp.domain.order.command.domain.aggregate.entity.Order;
import com.varc.brewnetapp.domain.order.command.domain.aggregate.entity.OrderItem;
import com.varc.brewnetapp.domain.order.command.domain.repository.OrderItemRepository;
import com.varc.brewnetapp.domain.order.command.domain.repository.OrderRepository;
import com.varc.brewnetapp.domain.returning.command.domain.aggregate.entity.ReturningItem;
import com.varc.brewnetapp.domain.returning.command.domain.repository.ReturningItemRepository;
import com.varc.brewnetapp.domain.storage.command.domain.aggregate.Stock;
import com.varc.brewnetapp.domain.storage.command.domain.repository.StockRepository;
import com.varc.brewnetapp.exception.DuplicateException;
import com.varc.brewnetapp.exception.EmptyDataException;
import com.varc.brewnetapp.exception.InvalidDataException;
import com.varc.brewnetapp.exception.MemberNotFoundException;
import com.varc.brewnetapp.security.utility.JwtUtil;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service(value = "commandDeliveryService")
@Slf4j
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryOrderStatusHistoryRepository deliveryOrderStatusHistoryRepository;
    private final DeliveryExchangeStatusHistoryRepository deliveryExchangeStatusHistoryRepository;
    private final DeliveryReturnStatusHistoryRepository deliveryReturnStatusHistoryRepository;
    private final StockRepository stockRepository;
    private final OrderItemRepository orderItemRepository;
    private final ExchangeItemRepository exchangeItemRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryExchangeRepository deliveryExchangeRepository;
    private final DeliveryReturnRepository deliveryReturnRepository;
    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final DelExStockRepository delExStockRepository;
    private final DelExStockItemRepository delExStockItemRepository;
    private final DelRefundRepository delRefundRepository;
    private final DelRefundItemRepository delRefundItemRepository;
    private final DelReStockRepository delReStockRepository;
    private final DelReStockItemRepository delReStockItemRepository;
    private final ReturningItemRepository returningItemRepository;

    @Autowired
    public DeliveryServiceImpl(
        DeliveryOrderStatusHistoryRepository deliveryOrderStatusHistoryRepository,
        DeliveryExchangeStatusHistoryRepository deliveryExchangeStatusHistoryRepository,
        DeliveryReturnStatusHistoryRepository deliveryReturnStatusHistoryRepository,
        StockRepository stockRepository, OrderItemRepository orderItemRepository,
        ExchangeItemRepository exchangeItemRepository,
        DeliveryOrderRepository deliveryOrderRepository,
        DeliveryExchangeRepository deliveryExchangeRepository,
        DeliveryReturnRepository deliveryReturnRepository, JwtUtil jwtUtil,
        MemberRepository memberRepository, DelExStockRepository delExStockRepository,
        DelExStockItemRepository delExStockItemRepository, DelRefundRepository delRefundRepository,
        DelRefundItemRepository delRefundItemRepository, DelReStockRepository delReStockRepository,
        DelReStockItemRepository delReStockItemRepository,
        ReturningItemRepository returningItemRepository) {
        this.deliveryOrderStatusHistoryRepository = deliveryOrderStatusHistoryRepository;
        this.deliveryExchangeStatusHistoryRepository = deliveryExchangeStatusHistoryRepository;
        this.deliveryReturnStatusHistoryRepository = deliveryReturnStatusHistoryRepository;
        this.stockRepository = stockRepository;
        this.orderItemRepository = orderItemRepository;
        this.exchangeItemRepository = exchangeItemRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.deliveryExchangeRepository = deliveryExchangeRepository;
        this.deliveryReturnRepository = deliveryReturnRepository;
        this.jwtUtil = jwtUtil;
        this.memberRepository = memberRepository;
        this.delExStockRepository = delExStockRepository;
        this.delExStockItemRepository = delExStockItemRepository;
        this.delRefundRepository = delRefundRepository;
        this.delRefundItemRepository = delRefundItemRepository;
        this.delReStockRepository = delReStockRepository;
        this.delReStockItemRepository = delReStockItemRepository;
        this.returningItemRepository = returningItemRepository;
    }




    @Transactional
    @Override
    public void createDeliveryStatus(
        CreateDeliveryStatusRequestDTO createDeliveryStatusRequestDTO, String accessToken) {

        String loginId = jwtUtil.getLoginId(accessToken.replace("Bearer ", ""));
        Member member = memberRepository.findById(loginId)
            .orElseThrow(() -> new MemberNotFoundException("회원의 토큰이 잘못되었습니다"));

        if(createDeliveryStatusRequestDTO.getDeliveryKind().equals(DeliveryKind.ORDER)){
            DeliveryOrderStatusHistory.OrderStatus status = null;

            if(createDeliveryStatusRequestDTO.getDeliveryStatus().equals((DeliveryStatus.SHIPPED))){
                status = DeliveryOrderStatusHistory.OrderStatus.SHIPPED;

                List<OrderItem> orderItems = orderItemRepository.findByOrderItemCode_OrderCode(createDeliveryStatusRequestDTO.getCode());

                if(orderItems.isEmpty() || orderItems == null)
                    throw new EmptyDataException("주문 건에 대한 상품이 없습니다");

                for (OrderItem orderItem : orderItems) {
                    orderItem.setAvailable(Available.AVAILABLE);
                    orderItemRepository.save(orderItem);
                }
            }
            else if (createDeliveryStatusRequestDTO.getDeliveryStatus().equals((DeliveryStatus.SHIPPING))){
                status = DeliveryOrderStatusHistory.OrderStatus.SHIPPING;
                DeliveryOrder order = deliveryOrderRepository.findById(createDeliveryStatusRequestDTO.getCode())
                    .orElseThrow(() -> new InvalidDataException("주문 코드를 잘못 입력했습니다"));

                order.setDeliveryCode(member.getMemberCode());

                deliveryOrderRepository.save(order);
                List<OrderItem> orderItems = orderItemRepository.findByOrderItemCode_OrderCode(createDeliveryStatusRequestDTO.getCode());

                if(orderItems.isEmpty() || orderItems == null)
                    throw new EmptyDataException("주문 건에 대해서 재고를 감소할 상품이 없습니다");

                for (OrderItem orderItem : orderItems) {
                    Stock stock = stockRepository.findByStorageCodeAndItemCode(1, orderItem.getOrderItemCode().getItemCode());

                    if(stock == null)
                        throw new EmptyDataException("감소하려는 상품에 대한 재고 정보가 존재하지 않습니다");

                    stock.setOutStock(stock.getOutStock() - orderItem.getQuantity());

                    stockRepository.save(stock);
                }

            }
            else 
                throw new InvalidDataException("잘못된 상태값을 입력하셨습니다");

            DeliveryOrderStatusHistory existDeliveryOrderStatusHistory
                = deliveryOrderStatusHistoryRepository.findByOrderCodeAndStatus(
                createDeliveryStatusRequestDTO.getCode(), status).orElse(null);

            if(existDeliveryOrderStatusHistory != null)
                throw new DuplicateException("이미 주문 내역에 해당 status 값이 존재합니다");

            DeliveryOrderStatusHistory deliveryOrderStatusHistory = DeliveryOrderStatusHistory.builder()
                .createdAt(LocalDateTime.now())
                .status(status)
                .active(true)
                .orderCode(createDeliveryStatusRequestDTO.getCode())
                .build();

            deliveryOrderStatusHistoryRepository.save(deliveryOrderStatusHistory);
        }
        else if(createDeliveryStatusRequestDTO.getDeliveryKind().equals(DeliveryKind.EXCHANGE)){

            ExchangeStatus status = null;

            if(createDeliveryStatusRequestDTO.getDeliveryStatus().equals((DeliveryStatus.SHIPPED))){
                status = ExchangeStatus.SHIPPED;
                List<ExchangeItem> exchangeItemList = exchangeItemRepository.findByExchangeItemCode_ExchangeCode(createDeliveryStatusRequestDTO.getCode());

                DelExStock delExStock = DelExStock.builder()
                    .exchangeCode(createDeliveryStatusRequestDTO.getCode())
                    .status(DelExStock.ExchangeStatus.TOTAL_INBOUND)
                    .manager("김민준")
                    .comment("전체입고입니다")
                    .confirmed(DelExStock.ConfirmationStatus.UNCONFIRMED)
                    .createdAt(LocalDateTime.now())
                    .active(true)
                    .build();

                DelExStock newDelExStock = delExStockRepository.save(delExStock);

                for(ExchangeItem exchangeItem : exchangeItemList){
                    DelExStockItem delExStockItem = DelExStockItem.builder()
                        .itemCode(exchangeItem.getExchangeItemCode().getItemCode())
                        .quantity(exchangeItem.getQuantity())
                        .restockQuantity(exchangeItem.getQuantity())
                        .exchangeStockHistoryCode(newDelExStock.getExchangeStockHistoryCode()).build();

                    delExStockItemRepository.save(delExStockItem);
                }

            }
            else if (createDeliveryStatusRequestDTO.getDeliveryStatus().equals((DeliveryStatus.SHIPPING))){
                status = ExchangeStatus.SHIPPING;

                DeliveryExchange exchange = deliveryExchangeRepository.findById(createDeliveryStatusRequestDTO.getCode())
                    .orElseThrow(() -> new InvalidDataException("교환 코드를 잘못 입력했습니다"));

                exchange.setDeliveryCode(member.getMemberCode());

                deliveryExchangeRepository.save(exchange);

                List<ExchangeItem> exchangeItems = exchangeItemRepository.findByExchangeItemCode_ExchangeCode(createDeliveryStatusRequestDTO.getCode());

                if(exchangeItems.isEmpty() || exchangeItems == null)
                    throw new EmptyDataException("교환 건에 대해서 재고를 감소할 상품이 없습니다");
                for (ExchangeItem exchangeItem : exchangeItems) {
                    Stock stock = stockRepository.findByStorageCodeAndItemCode(1, exchangeItem.getExchangeItemCode().getItemCode());

                    if(stock == null)
                        throw new EmptyDataException("감소하려는 상품에 대한 재고 정보가 존재하지 않습니다");

                    stock.setOutStock(stock.getOutStock() - exchangeItem.getQuantity());

                    stockRepository.save(stock);
                }
            }
            else if (createDeliveryStatusRequestDTO.getDeliveryStatus().equals((DeliveryStatus.PICKED))){
                status = ExchangeStatus.PICKED;

                DeliveryExchange exchange = deliveryExchangeRepository.findById(createDeliveryStatusRequestDTO.getCode())
                    .orElseThrow(() -> new InvalidDataException("교환 코드를 잘못 입력했습니다"));

                exchange.setDeliveryCode(null);

                deliveryExchangeRepository.save(exchange);
            }
            else if (createDeliveryStatusRequestDTO.getDeliveryStatus().equals((DeliveryStatus.PICKING))){
                status = ExchangeStatus.PICKING;

                DeliveryExchange exchange = deliveryExchangeRepository.findById(createDeliveryStatusRequestDTO.getCode())
                    .orElseThrow(() -> new InvalidDataException("교환 코드를 잘못 입력했습니다"));

                exchange.setDeliveryCode(member.getMemberCode());

                deliveryExchangeRepository.save(exchange);
            }

            else
                throw new InvalidDataException("잘못된 상태값을 입력하셨습니다");

            DeliveryExchangeStatusHistory existDeliveryexchangeStatusHistory
                = deliveryExchangeStatusHistoryRepository.findByExchangeCodeAndStatus(
                createDeliveryStatusRequestDTO.getCode(), status).orElse(null);

            if(existDeliveryexchangeStatusHistory != null)
                throw new DuplicateException("이미 교환 내역에 해당 status 값이 존재합니다");

            DeliveryExchangeStatusHistory deliveryExchangeStatusHistory = DeliveryExchangeStatusHistory.builder()
                .createdAt(LocalDateTime.now())
                .status(status)
                .active(true)
                .exchangeCode(createDeliveryStatusRequestDTO.getCode())
                .build();


            deliveryExchangeStatusHistoryRepository.save(deliveryExchangeStatusHistory);
        }
        else if(createDeliveryStatusRequestDTO.getDeliveryKind().equals(DeliveryKind.RETURN)){

            ReturnStatus status = null;

            if (createDeliveryStatusRequestDTO.getDeliveryStatus().equals((DeliveryStatus.PICKED))){
                DelReStock delReStock = DelReStock.builder()
                    .status(StockStatus.TOTAL_INBOUND)
                    .manager("김민준")
                    .comment("전체 입고입니다")
                    .confirmed(DelReStock.ConfirmationStatus.UNCONFIRMED)
                    .createdAt(LocalDateTime.now())
                    .active(true)
                    .returnCode(createDeliveryStatusRequestDTO.getCode())
                    .build();

                DelReStock newDelReStock = delReStockRepository.save(delReStock);

                DelRefund delRefund = DelRefund.builder()
                    .status(DelRefund.RefundStatus.TOTAL_REFUND)
                    .createdAt(LocalDateTime.now())
                    .manager("이수희")
                    .comment("전체 환불입니다")
                    .active(true)
                    .confirmed(DelRefund.ConfirmationStatus.UNCONFIRMED)
                    .returnCode(createDeliveryStatusRequestDTO.getCode())
                    .build();

                DelRefund newDelRefund = delRefundRepository.save(delRefund);

                List<ReturningItem> returningItemList = returningItemRepository.findByReturningItemCode_ReturningCode(createDeliveryStatusRequestDTO.getCode());

                for (ReturningItem returningItem : returningItemList){
                    DelRefundItem delRefundItem = DelRefundItem.builder()
                        .completed(true)
                        .itemCode(returningItem.getReturningItemCode().getItemCode())
                        .returnRefundHistoryCode(newDelRefund.getReturnRefundHistoryCode())
                        .build();

                    DelReStockItem delReStockItem = DelReStockItem.builder()
                        .itemCode(returningItem.getReturningItemCode().getItemCode())
                        .returnStockHistoryCode(newDelReStock.getReturnStockHistoryCode())
                        .quantity(returningItem.getQuantity())
                        .restockQuantity(returningItem.getQuantity())
                        .build();

                    delReStockItemRepository.save(delReStockItem);
                    delRefundItemRepository.save(delRefundItem);
                }

                status = ReturnStatus.PICKED;
            }

            else if (createDeliveryStatusRequestDTO.getDeliveryStatus().equals((DeliveryStatus.PICKING))){
                status = ReturnStatus.PICKING;

                DeliveryReturn deliveryReturn = deliveryReturnRepository.findById(createDeliveryStatusRequestDTO.getCode())
                    .orElseThrow(() -> new InvalidDataException("잘못된 반품 코드를 입력했습니다"));

                deliveryReturn.setDeliveryCode(member.getMemberCode());

                deliveryReturnRepository.save(deliveryReturn);
            }

            else
                throw new InvalidDataException("잘못된 상태값을 입력하셨습니다");

            DeliveryReturnStatusHistory existDeliveryReturnStatusHistory
                = deliveryReturnStatusHistoryRepository.findByReturnCodeAndStatus(
                createDeliveryStatusRequestDTO.getCode(), status).orElse(null);

            if(existDeliveryReturnStatusHistory != null)
                throw new DuplicateException("이미 반품 내역에 해당 status 값이 존재합니다");

            DeliveryReturnStatusHistory deliveryReturnStatusHistory = DeliveryReturnStatusHistory.builder()
                .createdAt(LocalDateTime.now())
                .status(status)
                .active(true)
                .returnCode(createDeliveryStatusRequestDTO.getCode())
                .build();


            deliveryReturnStatusHistoryRepository.save(deliveryReturnStatusHistory);
        }


    }
}
