package jpabook.jpa.shop;

import jpabook.jpa.shop.domain.Address;
import jpabook.jpa.shop.domain.Order;
import jpabook.jpa.shop.domain.OrderItem;
import jpabook.jpa.shop.domain.OrderStatus;
import jpabook.jpa.shop.repository.OrderRepository;
import jpabook.jpa.shop.repository.OrderSearch;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OrderAPIController {
//    private final Logger log = LoggerFactory.getLogger(OrderAPIController.class);

    private final OrderRepository orderRepository;

    /**
     * 엔티티를 직접 조회
     * - Jackson에 의해 Json으로 변환 되는 순간에 데이터가 꼬인다
     * - 결론은 Entity 반환 하면 안된다
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        log.info("order = {}", orders.toString());

        // 지연 로딩으로 인해 .getUsername | .getAddress를 하게 되면 Hibernate5에 의해 데이터 출력
        // 현재는 Hibernate5 옵션 꺼 두었음
        for (Order order : orders) {
            order.getMember().getUsername();
            order.getDelivery().getAddress();

            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }
        return orders;
    }

    /**
     * 엔티티를 DTO로 변환하여 조회
     * !! 개인적으로 보려고 주석 달아둔 것 뿐입니다.
     *
     * - Entity Type을 반환하는 것은 절대적으로 지양해야 하는 부분
     * - findAllByString
     *      -> Order(Entity)
     *          -> Loop
     *              -> toDTO(OrderDto)
     *                  -> setting data
     *                  -> OrderItem Entity 존재
     *                      -> Loop
     *                          -> toDTO(OrderItemDto)
     *                              -> setting data
     *                              -> API 스펙에 맞는 데이터만 출력 후 반환
     *
     */
    @GetMapping("/api/v2/orders")
    public List<?> ordersV2() {
        // orderRepository.findAll(); -> Order만 가져오는 경우는 상관이 없음, 현재 연관관계 -> Member > Order[ == Delivery ] > OrderItem > Item
//        return orderRepository.findAllByString(new OrderSearch()).stream()
//                .map(o -> new OrderDto(o))
//                .collect(Collectors.toList());

        List<Order> orderList = orderRepository.findAllByString(new OrderSearch());
        log.info("orderList = {}, size = {}", orderList.toString(), orderList.size());

        List<?> result = null;
        if (!CollectionUtils.isEmpty(orderList)) {
            log.info("orderList is not empty");
            result = orderList.stream()
                    .map(o -> new OrderDto(o)) // Order Entity --> OrderDto || ----> order.getOrderItems ----> OrderItemDto
                    .collect(Collectors.toList());
        }
        log.info("result = {}", result.toString());
        return result;
    }

    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        log.info("orders = {}, size = {}", orders.toString(), orders.size());

        for (Order order : orders) {
            log.info("order ref = {}, id = {}", order, order.getId());
        }

        List<OrderDto> order = orderRepository.findAllWithItem().stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        return order;
    }

    /**
     * 컬렉션 타입 -> fetch join pagination 사용
     *
     * @param offset
     * @param limit
     * @return
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);
        log.info("orders = {}", orders.toString());

        // ====> 1 : N fetch join
        // Order + Member + Delivery => query(1)
        // Loop -> OrderItem // Item => query(2)

        List<OrderDto> order = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        return order;
    }

    // test
    // default.batch_fetch_size : 100
    // -> 한 방에 긁어와서 셋팅 해둠
    // Collection 쪽에 @BetchSize(size = 100) 이런식으로도 가능
    @GetMapping("/api/v3.1.1/orders")
    public List<OrderDto> ordersV3_page_test(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {

        List<Order> orderList = orderRepository.findAllWithMemberDelivery(offset, limit); // Order + Member + Delivery = pagination
        log.info("orderList = {}", orderList);

        // query -> 1

        return orderList.stream()
                .map(order -> new OrderDto(order))
                .collect(Collectors.toList()); // query -> 2
    }

    @Getter
    static class OrderDto {
        private Long orderId;
        private String username;
        private LocalDateTime orderDate; // 주문일
        private OrderStatus orderStatus; // [ORDER, CANCEL]
        private Address address; // 값 타입
        private List<OrderItemDto> orders;

        public OrderDto(Order order) {
            orderId = order.getId();
            username = order.getMember().getUsername();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); // member.getAddress() 하면 회원 주소 정보 가져온다. delivery.getAddress() 해야 배송 정보 가져옴

            // 중요한 부분 체크 -> List<OrderItem> orderItems = new ArrayList<OrderItem>();
            // OrderItem -> 주문한 상품 정보
            orders = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }

    @Getter
    static class OrderItemDto {
        private String itemName;
        private int price;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            price = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
