package jpabook.jpa.shop;

import jpabook.jpa.shop.domain.*;
import jpabook.jpa.shop.domain.item.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

/**
 * 2명의 유저가 2개의 주문을 등록 한다 가정하고 샘플 데이터 입력
 *
 * @since 2022-04-27 Wed 01:01
 * @author ymkim
 */
@Component
@RequiredArgsConstructor
public class InitDB {

    private final InitService initService;

    /**
     * @PostConstruct
     *
     * 부연 설명
     * - DI(의존성 주입) 이후에 초기화를 수행
     * - bean lifecycle 에서 오직 한 번만 수행 됨을 보장해준다
     * - 즉, bean이 여러번 초기화 되는 것을 방지할 경우 사용
     */
    @PostConstruct
    public void init() {
        initService.dbInit1();
        initService.dbInit2();
    }

    /**
     * @Component
     *
     * 부연 설명
     * - 개발자가 직접 작성한 Class를 Bean으로 만드는 것
     * - 싱글톤 클래스 빈(Bean)을 생성하는 어노테이션
     * - 패키지 스캔 안에 '해당 클래스를 정의했으니 빈으로 등록하라' 라는 의미로 받아들이면 될 듯
     *     - ComponetScan : Component가 붙은 클래스를 전부 스캔
     *     - IOC 랑 연관 있음
     */
    @Component
    @Transactional // exec commit - rollback
    @RequiredArgsConstructor
    static class InitService {

        private final EntityManager em;

        public void dbInit1() {
            // 주문자 정보 생성
            Member member = getMember("userA", "서울", "1", "1111");
            em.persist(member);

            // 주문 상품 등록(실제로는 어드민이 상품 등록)
            Book book1 = createBook("JPA1 BOOk", 10000, 100);
            em.persist(book1);

            Book book2 = createBook("JPA2 BOOk", 20000, 100);
            em.persist(book2);

            // 생성 메서드 호출 -> OrderItem 생성(연관관계 설정)
            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 10000, 1);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 20000, 2);

            // 배송지 주소 1 : 1 생성
            Delivery delivery = createDelivery(member);

            // 생성 메서드 호출 -> Order에 엮여있는 엔티티들을 한 번에 묶고 반환 해준다
            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            em.persist(order);
        }

        public void dbInit2() {
            Member member = getMember("userB", "부산", "2", "2222");
            em.persist(member);

            Book book1 = createBook("SPRING1 BOOK", 20000, 200);
            em.persist(book1);

            Book book2 = createBook("SPRING2 BOOK", 40000, 300);
            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 10000, 3);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 20000, 4);

            Delivery delivery = createDelivery(member);

            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            em.persist(order);
        }

        private Delivery createDelivery(Member member) {
            Delivery delivery = new Delivery();
            delivery.setAddress(member.getAddress());
            return delivery;
        }

        private Member getMember(String username, String city, String street, String zipcode) {
            Member member = new Member();
            member.setUsername(username);
            member.setAddress(new Address(city, street, zipcode));
            return member;
        }

        private Book createBook(String name, int price, int stockQuantity) {
            Book book1 = new Book();
            book1.setName(name);
            book1.setPrice(price);
            book1.setStockQuantity(stockQuantity);
            return book1;
        }
    }
}

