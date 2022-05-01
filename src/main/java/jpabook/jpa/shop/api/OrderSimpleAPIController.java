package jpabook.jpa.shop.api;

import jpabook.jpa.shop.domain.Address;
import jpabook.jpa.shop.domain.Order;
import jpabook.jpa.shop.domain.OrderStatus;
import jpabook.jpa.shop.repository.OrderRepository;
import jpabook.jpa.shop.repository.OrderSearch;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
@Slf4j
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

    // TODO: V3 fetch 조인 최적화 진행
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> orderV2() {
        // JPQL에 의해 ORDER와 Member를 조인한 결과를 '2개' 반환 한다. 여기서 2개란 userA, userB가 주문한 내역을 의미 한다.
        // stream 사용 시 2번의 Loop를 돌게 되는데, 이 때 지연 로딩에 의해 n + 1 문제가 발생할 수 있다

        // (중요) N + 1 문제, fetch 조인으로 해결이 가능하나 현재는 사용 안함

        // 첫 번째 쿼리의 결과로 N 번 만큼 쿼리가 추가적으로 실행 되는 경우를 -> N + 1 상황
        // ex) 1 + 회원 N + 배송 N
        // ex) 1 + 회원 2 + 배송 2 ==> 5
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        log.debug("orders = {}", orders.toString());

        // 주문 결과가 2개, Loop가 2번 돈다
        List<SimpleOrderDto> result = orders.stream()
                .map(m -> new SimpleOrderDto(m))
                .collect(Collectors.toList());
        return result;
    }

    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> orderV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());
        return result;
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String username;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            this.orderId = order.getId();
            this.username = order.getMember().getUsername();
            this.orderDate = order.getOrderDate();
            this.orderStatus = order.getStatus();
            this.address = order.getDelivery().getAddress();
        }
    }
}
