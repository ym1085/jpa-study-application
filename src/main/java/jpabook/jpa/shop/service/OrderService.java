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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository MemberRepository;
    private final ItemRepository itemRepository;

    /**
     * 상품 주문
     *
     * @param memberId  : [Long] 회원 id(unq)값
     * @param itemId    : [Long] 상품 id(unq)값
     * @param count     : [int]  상품 갯수
     */
    @Transactional
    public Long order(Long memberId, Long itemId, int count) {
        // 엔티티 조회
        Member member = MemberRepository.findById(memberId);
        Item item = itemRepository.findById(itemId);

        // 배송 정보 생성
        Delivery delivery = new Delivery();
        delivery.setAddress(member.getAddress());

        // 주문 상품 생성
        OrderItem orderItem = OrderItem.createOrderItem(item, item.getPrice(), count);

        // 주문 생성
        Order order = Order.createOrder(member, delivery, orderItem);

        // 주문 저장
        orderRepository.save(order);
        return order.getId();
    }

    /**
     * 취소
     *
     * @param orderId  : [Long] 주문 id(unq)값
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        // 주문 엔티티 조회
        Order order = orderRepository.findById(orderId);

        // 주문 취소
        order.cancelOrder();
    }

    // 검색
    /*public List<Order> searchOrder(OrdeerSearch orderSearch) {
        return orderRepository.findAll(orderSearch);
    }*/
}
