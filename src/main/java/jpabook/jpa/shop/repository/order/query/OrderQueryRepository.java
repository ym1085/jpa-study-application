package jpabook.jpa.shop.repository.order.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class OrderQueryRepository {

    private final EntityManager em;

    public List<OrderQueryDto> findOrderQueryDtos() {
        List<OrderQueryDto> result = findOrders(); // 2 orders
        log.info("result = {}", result.toString());

        AtomicInteger idx = new AtomicInteger();
        result.forEach(o -> { // n + 1 문제 발생
            log.info("idx = {}", idx.incrementAndGet());
            List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId());
            o.setOrderItems(orderItems);
        });

        /*result.forEach(o -> {
            log.info("--------> oderItem = {}", o.getOrderItems()); //  --------> oderItem = null --> join 자체를 안 했기 때문에
            List<OrderItem> test = em.createQuery(
                    "select oi from OrderItem oi", OrderItem.class).getResultList();

            for (OrderItem orderItem : test) {
                log.info("xxxx orderItem = {}", orderItem.getOrderPrice());
                log.info("xxxx orderItem = {}", orderItem.getCount());
                log.info("xxxx orderItem = {}", orderItem.getOrder().getId());
            }
            log.info("--------> test = {}", test.toString());
        });*/

        return result;
    }

    public List<OrderQueryDto> findAllByDto_optimization() {
        // root 1번
        List<OrderQueryDto> result = findOrders(); // order inner join member, inner join delivery
        log.info("result = {}", result.toString());

        // collection 1번
        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(toOrderIds(result));
        log.info("orderItemMap = {}", orderItemMap.toString());

        result.stream().forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));
        return result;
    }

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {
        List<OrderItemQueryDto> orderItems = em.createQuery(
                                                      "select new jpabook.jpa.shop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                                                              " from OrderItem oi" +
                                                              " join oi.item i" +
                                                              " where oi.order.id in :orderIds", OrderItemQueryDto.class)
                                              .setParameter("orderIds", orderIds)
                                              .getResultList();

        Map<Long, List<OrderItemQueryDto>> orderItemMap =
                orderItems.stream().collect(Collectors.groupingBy(orderItemQueryDto -> orderItemQueryDto.getOrderId())); // 스트림의 요소를 특정 값을 기준으로 그룹화
        return orderItemMap;
    }

    private List<Long> toOrderIds(List<OrderQueryDto> result) {
        List<Long> orderIds = result.stream()
                                    .map(o -> o.getOrderId())
                                    .collect(Collectors.toList());
        return orderIds;
    }

    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        return em.createQuery(
                "select new jpabook.jpa.shop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                        " from OrderItem oi" +
                        " join oi.item i" +
                        " where oi.order.id = :orderId", OrderItemQueryDto.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    private List<OrderQueryDto> findOrders() {
        return em.createQuery(
                         "select new jpabook.jpa.shop.repository.order.query.OrderQueryDto(o.id, m.username, o.orderDate, o.status, d.address)" +
                                 " from Order o" +
                                 " join o.member m" +
                                 " join o.delivery d", OrderQueryDto.class)
                 .getResultList();
    }

    public List<OrderFlatDto> findAllByDto_flat() {
        return em.createQuery(
                "select new jpabook.jpa.shop.repository.order.query.OrderFlatDto(o.id, m.username, o.orderDate, o.status, d.address, i.name, oi.orderPrice, oi.count) " +
                        " from Order o" +
                        " join o.member m" +
                        " join o.delivery d" +
                        " join o.orderItems oi" + // 1 : N 조인은 데이터 뻥튀기 발생
                        " join oi.item i", OrderFlatDto.class)
                 .getResultList();
    }
}
