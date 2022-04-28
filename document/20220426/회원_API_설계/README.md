> 전체 회원 조회 API를 설계하던 중 나온 문제점

## 01. 응답 값으로 엔티티를 직접 외부에 노출한 경우

### 문제점

- 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다.
- 기본적으로 엔티티의 모든 값이 노출된다.
- 응답 스펙을 맞추기 위해 로직이 추가된다. (@JsonIgnore, 별도의 뷰 로직 등등)
- 실무에서는 같은 엔티티에 대해 API 용도에 따라 다양하게 만들어지는데, 한 엔티티에 각각의 API를 위한  
프레젠테이션 응답 로직을 담기는 어렵다.
- **엔티티가 변경되면 API 스펙이 변한다.**
- 추가로 컬렉션을 직접 반환하면 향후 API 스펙 변경이 어렵다.
  - 별도의 Result 클래스 생성으로 해결해야 한다.

### 결론

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberAPIController {
    private final Logger log = LoggerFactory.getLogger(MemberAPIController.class);
    
    private final MemberService memberService;
    
    // AS-IS
    // Member 엔티티 자체를 Response로 반환
    @GetMapping("/v1/members")
    public List<Member> findMembersV1() {
        return memberService.findAll();
    }
    
    // TO-BE
    @GetMapping("/v2/members")
    public List<?> findMembersV2() {
        //
    }
}
```

- API 응답 스펙에 맞추어 별도의 DTO를 반환 한다.
- 즉, 엔티티 자체를 반환 하는것이 아니라 별도의 DTO를 생성하여 반환 한다.

## 02. API 개발 고급

> API 최적화를 위한 방법 기재

- 조회용 샘플 데이터 입력
- 지연 로딩과 조회 성능 최적화
- 컬렉션 조회 최적화
- 페이징과 한계 돌파
- OSIV와 성능 최적화

### 02-1. 조회용 샘플 데이터 입력

> API 개발 고급 설명을 위한 샘플 데이터 입력

- **userA**
  - JPA1 BOOK
  - JPA2 BOOK
- **userB**
  - SPRING1 BOOK
  - SPRING2 BOOK

샘플 데이터 등록 소스는 아래와 같다.

```java
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
```