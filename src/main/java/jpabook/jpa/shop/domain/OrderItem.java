package jpabook.jpa.shop.domain;

import jpabook.jpa.shop.domain.item.Item;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id @GeneratedValue
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private int orderPrice; //주문 가격
    private int count; //주문 수량

    // 생성 메서드 작성
    public static OrderItem createOrderItem(Item item, int orderPrice, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setOrderPrice(orderPrice);
        orderItem.setCount(count);

        // 주문 하는거니까, 상품 재고 수량에서 -1
        item.removeStock(count); // count -> 주문 수량
        return orderItem;
    }

    // 비즈니스 로직
    public void cancelOrder() {
        getItem().addStock(count); // 현재 남아있는 재고수량을 받아옴
    }

    // 상품 총 주문 가격 반환
    public int getTotalPrice() {
        return getOrderPrice() * getCount();
    }
}
