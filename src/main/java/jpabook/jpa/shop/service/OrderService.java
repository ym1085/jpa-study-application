package jpabook.jpa.shop.service;

import jpabook.jpa.shop.domain.Delivery;
import jpabook.jpa.shop.domain.Member;
import jpabook.jpa.shop.domain.Order;
import jpabook.jpa.shop.domain.OrderItem;
import jpabook.jpa.shop.domain.item.Item;
import jpabook.jpa.shop.repository.ItemRepository;
import jpabook.jpa.shop.repository.MemberRepository;
import jpabook.jpa.shop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    /**
     * 상품 주문
     *
     * @param memberId
     * @param itemId
     * @param count
     * @return
     */
    @Transactional
    public Long order(Long memberId, Long itemId, int count) {
        // 유저(id), 상품명(id) -> 엔티티 조회
        Member member = memberRepository.findById(memberId); // 회원 번호에 해당하는 유저 조회
        Item item = itemRepository.findById(itemId);         // 상품 번호에 해당하는 상품 조회

        // 배송 정보 생성
        Delivery delivery = new Delivery();
        delivery.setAddress(member.getAddress());            // 회원 가입 시에 해당 회원의 정보가 있다 가정, 기본 정보 셋팅

        // 주문 상품 생성
        OrderItem orderItems = OrderItem.createOrderItem(item, item.getPrice(), count);

        // 주문 생성
        Order order = Order.createOrder(member, delivery, orderItems);
        log.debug("order = {}", order.toString());

        // 주문 저장
        orderRepository.save(order);
        return order.getId();
    }

    /**
     * 상품 주문 취소
     *
     * @param orderId
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId);
        log.trace("order = {}", order.toString());
        order.cancelOrder();
    }
}
