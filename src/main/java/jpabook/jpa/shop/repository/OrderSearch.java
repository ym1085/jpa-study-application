package jpabook.jpa.shop.repository;

import jpabook.jpa.shop.domain.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
public class OrderSearch {

    private String memberName;  //회원명
    private OrderStatus orderStatus; //주문 상태[ORDER, CANCEL]
}
