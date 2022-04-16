package jpabook.jpa.shop.service;

import jpabook.jpa.shop.domain.Address;
import jpabook.jpa.shop.domain.Member;
import jpabook.jpa.shop.domain.Order;
import jpabook.jpa.shop.domain.OrderStatus;
import jpabook.jpa.shop.domain.item.Book;
import jpabook.jpa.shop.domain.item.Item;
import jpabook.jpa.shop.repository.OrderRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class OrderServiceTest {

    @Autowired EntityManager em;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;

    @Test
    public void 상품주문() throws Exception {
        //given
        Member member = createMember();
        Item item = createItem(10000, 10, "JPA Application 만들기");
        int orderCount = 2; // 주문 갯수

        //when
        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);

        //then
        Order getOrder = orderRepository.findById(orderId);

        // 상품 주문에 관련된 모든 테스트 케이스를 작성해야 함
        assertEquals("상품 주문시 상태는 ORDER", OrderStatus.ORDER, getOrder.getStatus());
        assertEquals("주문한 상품 종류 수가 정확해야 한다", 1, getOrder.getOrderItems().size());
//        assertEquals("주문한 상품 종류 수가 정확해야 한다", 12, getOrder.getOrderItems().size());
        assertEquals("주문 가격은 가격 * 수량", 10000 * orderCount, getOrder.getTotalPrice());
        assertEquals("주문 수량만큼 재고가 줄어야 한다.", 8,  item.getStockQuantity());
    }

//    @Test(expected = NotEnoughStockException.class)
    @Test
    public void 상품주문수_현재_재고수량_초과() throws Exception {
        //given
        Member member = createMember();
        Item item = createItem(10000, 10, "JPA Application 만들기");

        int orderCount = 11; // 주문 수량

        //when
        Long order = orderService.order(member.getId(), item.getId(), orderCount);

        //then
        fail("재고 수량 부족 예외가 발생 해야 한다.");
    }

    @Test
    public void 주문취소() throws Exception {
        //given
        Member member = createMember();
        Item item = createItem(10000, 10, "T-SQL 실습 과정 구현하기");

        int orderCount = 2;

        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);

        //when
        orderService.cancelOrder(orderId);

        //then
        Order getOrder = orderRepository.findById(orderId);
        assertEquals("주문 취소시 상태는 CANCLE 이다.", OrderStatus.CANCEL, getOrder.getStatus());
        assertEquals("주문이 취소된 상품은 그만큼 재고가 증가해야 한다.", 10, item.getStockQuantity());

//        assertEquals("주문 취소시 상태는 CANCLE 이다.", OrderStatus.ORDER, getOrder.getStatus());
//        assertEquals("주문이 취소된 상품은 그만큼 재고가 증가해야 한다.", 1022, item.getStockQuantity());
    }

    private Item createItem(int price, int stockQuantity, String productName) {
        Item item = new Book();
        item.setPrice(price);   // 가격
        item.setStockQuantity(stockQuantity); // 재고량
        item.setName(productName); //  구매 상품명
        em.persist(item);
        return item;
    }

    private Member createMember() {
        Member member = new Member();
        member.setUsername("김영민");
        member.setAddress(new Address("경기도", "도로", "144223"));
        em.persist(member);
        return member;
    }
}