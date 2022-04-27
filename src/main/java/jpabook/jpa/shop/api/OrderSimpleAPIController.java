package jpabook.jpa.shop.api;

import jpabook.jpa.shop.domain.Order;
import jpabook.jpa.shop.repository.OrderRepository;
import jpabook.jpa.shop.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * XToOne(ManyToOne, OneToOne) -> Lazy Loading
 *
 * 필요한 정보
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleAPIController {

    private final OrderRepository orderRepository;

    // 엔티티 반환
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        for (Order order : orders) {
            order.getMember().getUsername();
            order.getDelivery().getAddress();
        }
        return orders;
    }
}
