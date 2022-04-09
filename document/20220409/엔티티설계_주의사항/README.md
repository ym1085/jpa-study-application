```
@date   : 2022-04-09 13:39
@author : ymkim
@desc   : 엔티티 설계 관련 주의 사항 간단히 정리
```

## 01. 엔티티 설계시 주의점

### 엔티티에는 가급적 Setter를 사용하지 말자

엔티티 설계 시 Setter가 모두 열려있는 경우 변경 포인트가 너무 많아져 유지보수가 어려워진다.  
즉, Getter는 열어두고 Setter는 가급적이면 사용을 지양하자.

### 모든 연관관계는 지연로딩으로 설정

```java
@Entity
@Table(name = "orders")
public class Order {
    // 기본 => @ManyToOne(fetch = FetchType.EAGER)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
}
```

- 즉시로딩(`EAGER`)은 예측이 어렵고, 어떤 SQL이 실행될지 추척하기 어렵다.
    - 특히 JPQL을 실행할 때 N + 1의 문제가 발생할 수 있다.
- 실무에서는 모든 연관관계를 지연로딩(`LAZY`)으로 설정해야 한다.
- 연관된 엔티티를 함께 DB에서 조회해야 하면, `fetch join` 또는 `엔티티 그래프 기능`을 사용한다.

```java
@Target({METHOD, FIELD}) 
@Retention(RUNTIME)

public @interface ManyToOne {

    /** 
     * (Optional) The entity class that is the target of 
     * the association. 
     *
     * <p> Defaults to the type of the field or property 
     * that stores the association. 
     */
    Class targetEntity() default void.class;

    /**
     * (Optional) The operations that must be cascaded to 
     * the target of the association.
     *
     * <p> By default no operations are cascaded.
     */
    CascadeType[] cascade() default {};

    /** 
     * (Optional) Whether the association should be lazily 
     * loaded or must be eagerly fetched. The EAGER
     * strategy is a requirement on the persistence provider runtime that 
     * the associated entity must be eagerly fetched. The LAZY 
     * strategy is a hint to the persistence provider runtime.
     */
    FetchType fetch() default EAGER;

    /** 
     * (Optional) Whether the association is optional. If set 
     * to false then a non-null relationship must always exist.
     */
    boolean optional() default true;
}
```

- @xToOne(OneToOne, ManyToOne) 관계는 기본 설정이 `즉시로딩`, 직접 `지연로딩`으로 설정해야 한다.
- 즉, @xToOne 어노테이션을 가진 부분은 반드시 `지연 로딩`으로 설정 해야 한다.

### 컬렉션은 필드에서 초기화 하자

```java
@Entity
@Getter @Setter
public class Member {
    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();
}
```

- `컬렉션은 필드에서 바로 초기화 하는 것이 안전하다.`
  - nulll 문제 방지
- 하이버네이트는 엔티티를 영속화 할 때, 컬랙션을 감싸서 하이버네이트가 제공하는 내장 컬렉션으로 변경한다. 만약 getOrders() 처럼 임의의 메서드에서 컬력션을 잘못 생성하면 하이버네이트 내부 메커니즘에 문제가 발생할 수 있다. 따라서 필드레벨에서 생성하는 것이 가장 안전하고, 코드도 간결하다.

```java
Member member = new Member();
System.out.println(member.getOrders().getClass());
em.persist(team);
System.out.println(member.getOrders().getClass());

// 출력 결과
class java.util.ArrayList
class org.hibernate.collection.internal.PersistentBag
```

### 테이블, 컬렴명 생성 전략

- 스프링 부트에서 하이버네이트 기본 매핑 전략을 변경해서 실제 테이블 필드명은 다름

**하이버네이트 기존 구현**

> 엔티티의 필드명을 그대로 테이블의 컬럼명으로 사용 (SpringPhysicalNamingStrategy)

**스프링 부트의 신규 설정**

> 엔티티(필드) -> 테이블(컬럼)

1. 카멜 케이스 -> 언더스코어(memberPoing -> member_point)
2. .(점) -> _(언더스코어)
3. 대문자 -> 소문자

**스프링 부트의 기본 설정**

```properties
# 논리명 생성 : 명시적으로 컬럼, 테이블명을 직접 적지 않으면 ImplicitNamingStrategy 사용
spring.jpa.hibernate.naming.implicit-strategy:
org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy
```

```properties
# 물리명 적용 : 모든 논리명에 적용됨, 실제 테이블에 적용
spring.jpa.hibernate.naming.physical-strategy:
org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy
```

### 영속성 전이 : CASCADE

> [[블로그] 영속성 전이 정리 내용](https://ym1085.github.io/jpa/JPA-%EC%98%81%EC%86%8D%EC%84%B1%EC%A0%84%EC%9D%B4-CASCADE/)

- 모든 엔티티는 저장을 하려면 각각 persist(영속화)를 해줘야 한다.
- 영속성 전이는 특정 엔티티를 영속 상태로 만들 때 연관된 엔티티도 함께 영속상태로 만들 때 사용. 
- 영속성 전이는 소유자가 하나인 경우에만 사용을 해야 한다.
- 영속성 전이와 관련하여 고아 객체라는 개념도 존재함.

### 연관관계 편의 메서드

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

//    @ManyToOne(fetch = FetchType.EAGER)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<OrderItem>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // 주문 상태 [ORDER, CANCEL]

    //==연관관계 편의 메서드==//
    public void setMember(Member member) {
        this.member = member;
        member.getOrders().add(this);
    }

    //==연관관계 편의 메서드==//
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    //==연관관계 편의 메서드==//
    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
        delivery.setOrder(this);
    }
}

```

- 양방향 연관관계에서 연관관계 편의 메서드를 사용하는 것이 좋다.


### 참고 링크

- [[문서] Spring Boot Reference Guide](https://docs.spring.io/spring-boot/docs/2.1.3.RELEASE/reference/htmlsingle/#howtoconfigure-hibernate-naming-strategy)
- [[문서] Hibernate Naming strategy](http://docs.jboss.org/hibernate/orm/5.4/userguide/html_single/Hibernate_User_Guide.html#naming)
- [[문서] GivenWhenThen](https://martinfowler.com/bliki/GivenWhenThen.html)