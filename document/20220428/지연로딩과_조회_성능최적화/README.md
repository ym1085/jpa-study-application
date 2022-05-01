## 01. 지연 로딩과 조회 성능 최적화

- 주문 + 배송정보 + 회원을 조회하는 API를 만든다
- 지연 로딩으로 인해 발생하는 성능 문제를 단계별로 파악

### 01-1. 주문 정보 조회

xToOne은 기본 로딩 전략이 EAGER이다. 즉, xToOne 어노테이션을 사용하는 경우 로딩 전략을 LAZY로 지정을 해줘야 한다는 말이다. 이번에는 주문 + 배송 + 회원 정보를 조회하는 API를 생성 해보자.

```java
@RestController
@RequiredArgsConstructor
public class OrderSimpleAPIController {
    
    private final OrderRepository orderRepository;
    
    @GetMapping("/api/v1/order-simple")
    public List<Order> ordersV1() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        return orders;
    }
}
```

주문 + 배송 + 회원 정보를 가져오기 위한 API ordersV1을 만들었다. 해당 코드를 간략히 설명하자면 orderRepository 참조 변수를
통해 회원의 주문 정보를 가져오는 상황이다. 그렇다면 현재 Order, Member, Delivery의 연관관계가 어떻게 설정이 되어 있는지 살펴보자.

```java
@Entity
public class Order {

    //...
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    public Member member;
    
    //...
    
}
```

```java
@Entity
public class Member {
    
    //...
    
    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<Order>();
    
    //...
}
```

```java
@Entity
public class Delivery {
    
    @OneToOne(mappedBy = "delivery", fetch = FetchType.LAZY)
    public Order order;
}
```

현재 Member와 Order 관계는 1 : N, Delivery와 Order의 관계는 1 : 1 인 상황이다.  
그렇다면, 위에서 findAllByString() 했을 경우 어떤 결과가 나오는지 살펴보자.

```java
@GetMapping("/api/v1/order-simple")
public List<Order> ordersV1() {
    List<Order> orders = orderRepository.findAllByString(new OrderSearch());
    return orders;
}
```

- 현재 이 부분을 의미하고 있다

![order_api_issue](../../../images/order_api_issue.png)

회원, 주문, 배송 정보는 서로 연관관계를 형성하고 있으며 지연 로딩까지 설정이 되어 있기 때문에  
무한 루프에 빠지게 된다. 객체를 JSON으로 만드는 Jackson 라이브러리 입장에서 Member에도 Order가  
있고, Order에도 Member가 있기 때문에(양방향 연관관계의 문제) 이러한 문제가 발생한다.

> 그렇다면 이러한 상황을 어떻게 해결해야 할까?

엔티티를 직접 노출할 때는 양방향 연관관계 걸린 곳은 꼭 한 쪽을 @JsonIgnore으로 설정 해주어야 한다. 여기서 양방향
연관 관계란 Member -> Order, Order -> Member 가 가능하다는 의미다.

```java

// Member
@JsonIgnore
@OneToMany(mappedBy = "member")
private List<Order> orders = new ArrayList<>();

----

// OrderItem
@JsonIgnore
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id")
private Order order;

----

// Delivery
@JsonIgnore
@OneToOne(mappedBy = "delivery", fetch = FetchType.LAZY)
private Order order;

```

- 양방향이 걸리는 부분은 @JsonIgnore를 통해 문제를 해결해야 한다

> 하지만 두 번째 문제가 발생한다.

```
"org.springframework.http.converter.HttpMessageConversionException: 
Type definition error: 
[simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]; 
nested exception is com.fasterxml.jackson.databind.exc.InvalidDefinitionException: 
No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor and 
no properties discovered to create BeanSerializer
```

위와 같은 에러가 발생하는 이유는 Member의 로딩 전략은 LAZY 타입이기 때문이다. 즉, 지연 로딩을 사용하는 경우 실제 객체를 가져오는 것이 아닌 프록시 객체를 가져오게 되는데, 
Jackson 라이브러리가 실 객체가 아닌 프록시 객체를 가져올 수 없기 때문에 위와 같은 예외가 발생하게 된다.

### Hibernate5Module 디팬던시 주입

**build.gradle**

```groovy
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-hibernate5'
```

- 실제 해당 디팬던시를 사용할 경우는 적다.

**Application.java**

```java
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

//    @Bean
//    Hibernate5Module hibernate5Module() {
//        return new Hibernate5Module();
//    }

    @Bean
    Hibernate5Module hibernate5Module() {
        Hibernate5Module hibernate5Module = new Hibernate5Module();
        hibernate5Module.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true);
        return hibernate5Module;
    }
}
```

Hibernate5Module을 Bean으로 등록하여 지연 로딩인 데이터를 안 가져올 수 도 있고, 설정을 통해
지연 로딩 데이터를 한 번에 가져올 수도 있다.

```json
[
    {
        "id": 4,
        "member": {
            "id": 1,
            "username": "userA",
            "address": {
                "city": "서울",
                "street": "1",
                "zipcode": "1111"
            }
        },
        "orderItems": [
            {
                "id": 6,
                "item": {
                    "id": 2,
                    "name": "JPA1 BOOk",
                    "price": 10000,
                    "stockQuantity": 99,
                    "categories": [],
                    "author": null,
                    "isbn": null
                },
                "orderPrice": 10000,
                "count": 1,
                "totalPrice": 10000
            },
            {
                "id": 7,
                "item": {
                    "id": 3,
                    "name": "JPA2 BOOk",
                    "price": 20000,
                    "stockQuantity": 98,
                    "categories": [],
                    "author": null,
                    "isbn": null
                },
                "orderPrice": 20000,
                "count": 2,
                "totalPrice": 40000
            }
        ],
        "delivery": {
            "id": 5,
            "address": {
                "city": "서울",
                "street": "1",
                "zipcode": "1111"
            },
            "status": null
        },
        "orderDate": "2022-04-28T20:47:38.790091",
        "status": "ORDER",
        "totalPrice": 50000
    },
    {
        "id": 11,
        "member": {
            "id": 8,
            "username": "userB",
            "address": {
                "city": "부산",
                "street": "2",
                "zipcode": "2222"
            }
        },
        "orderItems": [
            {
                "id": 13,
                "item": {
                    "id": 9,
                    "name": "SPRING1 BOOK",
                    "price": 20000,
                    "stockQuantity": 197,
                    "categories": [],
                    "author": null,
                    "isbn": null
                },
                "orderPrice": 10000,
                "count": 3,
                "totalPrice": 30000
            },
            {
                "id": 14,
                "item": {
                    "id": 10,
                    "name": "SPRING2 BOOK",
                    "price": 40000,
                    "stockQuantity": 296,
                    "categories": [],
                    "author": null,
                    "isbn": null
                },
                "orderPrice": 20000,
                "count": 4,
                "totalPrice": 80000
            }
        ],
        "delivery": {
            "id": 12,
            "address": {
                "city": "부산",
                "street": "2",
                "zipcode": "2222"
            },
            "status": null
        },
        "orderDate": "2022-04-28T20:47:38.836524",
        "status": "ORDER",
        "totalPrice": 110000
    }
]
```

- 해당 옵션을 적용하는 경우, 위와 같은 데이터 결과를 얻을 수 있다
- 현재는 FORCE_LAZY_LOADING이기 때문에 LAZY로 설정이 되어도 데이터를 다 가져온다
- `즉, Entity를 외부로 직접 노출하지 말라는 의미다`
- 간단한 Application이 아닌 이상 Entity를 절대 API 응답으로 반환하면 안된다
    - 반환 할거면 DTO로 엔티티를 변환 한 후에 반환 해야 한다

## 02. 엔티티 DTO로 반환

이번에는 엔티티를 DTO로 변환하는 V2 API를 만들어보자.

```java
@RestController
@RequiredArgsConstructor
@Slf4j
public class OrderSimpleAPIController {

    private final OrderRepository orderRepository;
    
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> orderV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        log.debug("orders = {}", orders.toString());
  
        List<SimpleOrderDto> result = orders.stream()
                  .map(m -> new SimpleOrderDto(m))
                  .collect(Collectors.toList());
        return result;
    }
  
    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String username;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
    
        public SimpleOrderDto(Order order) {
            this.orderId = order.getId();
            this.username = order.getMember().getUsername();
            this.orderDate = order.getOrderDate();
            this.orderStatus = order.getStatus();
            this.address = order.getDelivery.getAddress();
        }
    }
}
```

기존 V1 API에서는 DTO로 변환하는 과정 없이 엔티티 자체를 반환 결과로 제공 하였다. 
이번에는 SimpleOrderDto라는 DTO를 생성한 결과를 반환 해주는 예제다.

```json
[
    {
        "orderId": 4,
        "username": "userA",
        "orderDate": "2022-04-28T21:11:18.49015",
        "orderStatus": "ORDER",
        "address": {
            "city": "서울",
            "street": "1",
            "zipcode": "1111"
        }
    },
    {
        "orderId": 11,
        "username": "userB",
        "orderDate": "2022-04-28T21:11:18.532793",
        "orderStatus": "ORDER",
        "address": {
            "city": "부산",
            "street": "2",
            "zipcode": "2222"
        }
    }
]
```

- DTO 결과값은 위와 같다

> 이번에는 기존 @JsonIgnore와 Hibernate5Module의 설정 값을 지운 후 요청을 날려 보았다.

```java
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

    // Hibernate5Module 관련 설정 주석 처리
//	@Bean
//	Hibernate5Module hibernate5Module() {
//		Hibernate5Module hibernate5Module = new Hibernate5Module();
//		hibernate5Module.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true); // 사용 지양 해야 함
//		return hibernate5Module;
//	}
}
```

```java
//    @JsonIgnore
@OneToOne(mappedBy = "delivery", fetch = FetchType.LAZY)
private Order order;
```

```java
//    @JsonIgnore
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id")
private Order order;
```

```java
//    @JsonIgnore
@OneToMany(mappedBy = "member")
private List<Order> orders = new ArrayList<>();
```

- 혹시나 하는 마음에 모든 @JsonIgnore와 Hibernate5Module을 제거한 후 다시 테스트 진행
- 결과는 이전과 동일하다

```json
[
    {
        "orderId": 4,
        "username": "userA",
        "orderDate": "2022-04-28T21:11:18.49015",
        "orderStatus": "ORDER",
        "address": {
            "city": "서울",
            "street": "1",
            "zipcode": "1111"
        }
    },
    {
        "orderId": 11,
        "username": "userB",
        "orderDate": "2022-04-28T21:11:18.532793",
        "orderStatus": "ORDER",
        "address": {
            "city": "부산",
            "street": "2",
            "zipcode": "2222"
        }
    }
]
```

> 여기서 중요한 부분을 다시 한번 짚고 넘어가자 

1. 엔티티를 직접 반환하는 경우, 한 쪽은 반드시 @JsonIgnore를 선언해야 한다
2. 만약 지연 로딩이 있는 경우 Hibernate5Module 설정을 따로 지정 해줘야 한다
3. `하지만 엔티티를 DTO로 변환하여 사용하게 되면, 위와 같은 부분을 신경 쓸 필요가 없다`
4. 위와 같이 사용하는 경우, 엔티티가 변해도 API 스펙이 변할 일이 없다

> 하지만 현재 지연 로딩으로 인해 데이터베이스 쿼리가 너무 많이 요청되는 경우가 있다

```log
2022-04-28 21:25:34.153  INFO 47804 --- [nio-8080-exec-3] j.jpa.shop.repository.OrderRepository    : jpql query = select o from Order o join o.member m
2022-04-28 21:25:34.154  INFO 47804 --- [nio-8080-exec-3] j.jpa.shop.repository.OrderRepository    : query = org.hibernate.query.internal.QueryImpl@4737c051
2022-04-28 21:25:34.154 DEBUG 47804 --- [nio-8080-exec-3] org.hibernate.SQL                        : 
    select
        order0_.order_id as order_id1_6_,
        order0_.delivery_id as delivery4_6_,
        order0_.member_id as member_i5_6_,
        order0_.order_date as order_da2_6_,
        order0_.status as status3_6_ 
    from
        orders order0_ 
    inner join
        member member1_ 
            on order0_.member_id=member1_.member_id limit ?
```

우선 inner join은 무시하고 select - from 절을 살펴보자.  
일단 order 테이블에서 데이터를 긁어와서 반환을 해준다.

```java
public class OrderSimpleController {
    
    //... 중략
    
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> orderV2() {
        // JPQL에 의해 ORDER와 Member를 조인한 결과를 '2개' 반환 한다. 여기서 2개란 userA, userB가 주문한 내역을 의미 한다.
        // stream 사용 시 2번의 Loop를 돌게 되는데, 이 때 지연 로딩에 의해 n + 1 문제가 발생할 수 있다
  
        // (중요) N + 1 문제, fetch 조인으로 해결이 가능하나 현재는 사용 안함
  
        // 첫 번째 쿼리의 결과로 N 번 만큼 쿼리가 추가적으로 실행 되는 경우를 -> N + 1 상황
        // ex) 1 + 회원 N + 배송 N
        // ex) 1 + 회원 2 + 배송 2 ==> 5
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        log.debug("orders = {}", orders.toString());

        // 절대적으로 DTO로 변환해서 보내야 한다
        // 주문 결과가 2개, Loop가 2번 돈다
        List<SimpleOrderDto> result = orders.stream()
                .map(m -> new SimpleOrderDto(m)) // stream을 통해 Loop 를 돌게 되면 n + 1 문제 발생
                .collect(Collectors.toList());
        return result;
    }
  
    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String username;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

      /**
       * LAZY(지연 로딩)가 초기화 되는 경우, 영속성 컨텍스트가 member_id를 가지고
       * 영속성 컨텍스트를 찾아본 다음 없으면 DB에 데이터를 날리게 된다.
       * 
       * @param order
       */
      public SimpleOrderDto(Order order) {
            this.orderId = order.getId();
            this.username = order.getMember().getUsername(); // LAZY 초기화
            this.orderDate = order.getOrderDate();
            this.orderStatus = order.getStatus();
            this.address = order.getDelivery().getAddress();  // LAZY 초기화
        }
    }    
}
```

- 쿼리가 1 + N + N 번 실행되는 소스
  - order 조회가 `1`
  - order -> member 지연 로딩 조회 `N`번
  - order -> delivery 지연 로딩 조회 `N`번
  - 예) order의 결과가 4개면 최악의 경우 1 + 4 + 4번 실행 된다
    - 지연 로딩은 영속성 컨텍스트에서 조회, 이미 조회된 경우 쿼리 생략

```java
2022-04-28 21:25:34.158 DEBUG 47804 --- [nio-8080-exec-3] org.hibernate.SQL                        : 
    select
        member0_.member_id as member_i1_4_0_,
        member0_.city as city2_4_0_,
        member0_.street as street3_4_0_,
        member0_.zipcode as zipcode4_4_0_,
        member0_.username as username5_4_0_ 
    from
        member member0_ 
    where
        member0_.member_id=? // id 값으로 질의
```

- **영속성 컨텍스트에 의해 2번째 쿼리가 날라간 로그**
- this.username = order.getMember().getUsername(); // LAZY 초기화

```log
2022-04-28 22:31:40.542 DEBUG 55836 --- [nio-8080-exec-1] org.hibernate.SQL                        : 
    select
        delivery0_.delivery_id as delivery1_2_0_,
        delivery0_.city as city2_2_0_,
        delivery0_.street as street3_2_0_,
        delivery0_.zipcode as zipcode4_2_0_,
        delivery0_.status as status5_2_0_ 
    from
        delivery delivery0_ 
    where
        delivery0_.delivery_id=?
```

- 세 번째로 지연 로딩 Delivery에 의해 쿼리가 나가게 된다
- this.address = order.getDelivery().getAddress(); // 해당 영역

## 03. Fetch join

```java
em.createQuery("select o from Order o" +
        " join fetch o.member m" +
        " join fetch o.delivery d").getResultList();
```

## 03. JPA DTO 바로 조회

```java
@Repository
@RequiredArgsConstruct
public class OrderSimpleQueryRepository {
    
    private final EntityManager em;
    
    public List<OrderSimpleQueryDto> findOrderDtos() {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.username, o.orderDate, o.status, d.address)" +
                        " from Order o" +
                        " join o.member m" +
                        " join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();
    }
}
```

```java
@Data
public class OrderSimpleQueryDto {
    private Long orderId;
    private String username;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;

    public OrderSimpleQueryDto(Long orderId, String username, LocalDateTime orderDate, OrderStatus orderStatus, Address address) {
        this.orderId = orderId;
        this.username = username;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address;
    }
}
```

- 일반적인 SQL을 사용할 때 처럼 원하는 값을 선택해서 조회
- new 명령어를 사용해서 JPQL의 결과를 DTO로 즉시 변환
- SELECT 절에서 원하는 데이터를 직접 선택하므로 DB -> 애플리케이션 네트웍 용량 최적화
- 리포지토리 재사용성 떨어짐, API 스펙에 맞춘 코드가 리포지토리에 들어가는 단점
- 논리적인 계층이 깨진 경우

### 03-1. 정리

엔티티를 DTO로 변환하거나, DTO로 바로 조회하는 두가지 방법은 각각 장단점이 있다. 둘중 상황에 따라서 더 나은
방법을 선택하면 된다. 엔티티로 조회하면 리포지토리 재사용성도 좋고, 개발도 단순해진다. 따라서 권장하는 방법은
다음과 같다.

쿼리 방식 선택 권장 순서

- 우선 엔티티를 DTO로 변환하는 방법을 선택한다
- 필요하면 페치 조인으로 성능을 최적화 한다. -> 대부분의 성능 이슈가 해결된다
- 그래도 안되면 DTO로 직접 조회하는 방법을 사용한다
- 최후의 방법은 JPA가 제공하는 네이티브 SQL이나 스프링 JDBC Template을 사용해서 SQL을 직접 사용한다

