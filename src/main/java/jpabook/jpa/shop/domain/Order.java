package jpabook.jpa.shop.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Setter @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<OrderItem>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // 주문 상태 [ORDER, CANCEL]

    private LocalDateTime orderDate;

    // 연관관계 편의 메서드
    public void setMember(Member member) {
        this.member = member;
        member.getOrders().add(this);
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
        delivery.setOrder(this);
    }

    // [생성자 메서드 선언]
    //
    // DDD(Domain Driven Develop) -> 도메인 주도 개발 -> 엔티티에 비즈니스 로직을 전부 작성한다
    // ----> 현재 Order 객체랑 연관되어 있는 얘들을 생성
    //
    // 연관 관계 파악
    // 1(Member) : N(Order)
    // N(Order) : 1(OrderItem)
    // 1(Order) : 1(Delivery)
    public static Order createOrder(Member member, Delivery delivery, OrderItem... orderItems) {
        Order order = new Order();
        order.setMember(member);
        order.setDelivery(delivery);
        for (OrderItem orderItem : orderItems) {
            order.addOrderItem(orderItem); // 연관관계 편의 메서드
        }
        // 남은 필드 초기화 => status(주문 상태), orderDate(주문 일자)
        order.setStatus(OrderStatus.ORDER); // ORDER, CANCEL
        order.setOrderDate(LocalDateTime.now());
        return order;
    }

    /**
     * 주문 취소
     */
    public void cancelOrder() {
        // 상태가 주문 취소로 변경 되어야 한다, 하지만 배달 상태가 완료면 주문이 불가능하기에 Exception을 던진다
        if (delivery.getStatus() == DeliveryStatus.COMP) {
            throw new IllegalStateException("주문한 상품이 배달이 완료되었기 때문에 취소가 불가능합니다.");
        }
        this.status = OrderStatus.CANCEL; // 주문 상태를 cancel 로 변경

        // 단일 취소만 가능하며, '상품 재고'가 하나 증가 하여야 한다.
        // 현재 Order는 상품 재고를 들고 있지 않음, OrderItem도 마찬가지(주문 수량, 주문 가격)다
        for (OrderItem orderItem : orderItems) {
            orderItem.cancelOrder();
        }
    }

    /**
     * 전체 상품 주문 가격 조회
     */
    public int getTotalPrice() {
        // OrderItem이 재고, 수량 들고 있음
        int totalPrice = 0;
        for (OrderItem orderItem : orderItems) {
            totalPrice += orderItem.getTotalPrice(); // 여러개 일수도 있으니까, Loop 안에서 더해준다
        }
        return totalPrice;
    }
}
