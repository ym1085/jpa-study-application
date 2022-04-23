package jpabook.jpa.shop.repository;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
public class OrderSearch {

    private String memberName;  //회원명
    private String orderStatus; //주문 상태[ORDER, CANCEL]
}
