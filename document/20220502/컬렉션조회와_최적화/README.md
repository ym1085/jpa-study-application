## 01. API 개발 고급 - 컬렉션 조회 최적화

```java
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
@Entity
public class Order {

    //..
    
    // 여기서 orders 참조 변수를 fetch join 에 사용 하는 경우를 말한다
//    @BatchSize(size = 1000)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<OrderItem>();
    
    //..
}
```

```sql
@Repository
@RequiredArgsConstructor
public class OrderRepository {
    
    private final EntityManager em;

    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        // xToOne(OneToOne, ManyToOne) 관계는 페이징에 영향을 주지 않는다.
        // 해당 연관관계 설정 시 fetch join을 사용 하여도 데이터 뻥튀기가 되지 않음.

        return em.createQuery(
                "select o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d", Order.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}
```

주문 내역에서 추가로 주문한 상품 정보를 추가로 조회하자.  
Order 기준으로 컬렉션인 `OrderItem`와 `Item`이 필요하다.

앞의 예제에서는 xToOne(OneToOne, ManyToOne) 관계만 있었다.  
이번에는 `컬렉션`인 `일대다 관계`(OneToMany)를 조회하고, 최적화 하는 방법을 알아보자.

**일대다 관계란**

1 = Order -> N = OrderItem

### 01-1. 컬렉션 fetch join

1. 한 개 이상의 컬렉션을 fetch join 하는 것은 절대적으로 피해야 한다.
2. oneToMany, ManyToMany fetch join 에서는 `페이징 처리가 불가능 하다`.

## 02. 엔티티를 DTO로 변환 - 페이징과 한계 돌파

컬렉션 fetch join 시 페이징 처리 하는 방법 기재

### 02-1. 페이징과 한계 돌파

> 컬렉션 1 : N 패치 조인 사용은 정말 조심해야 하는 부분  
> OutOfMemory 떠서 서버 뻗을 수 있음

- 컬렉션을 페치 조인하면 페이징이 불가능하다.
  - `컬렉션을 페치 조인하면 일대다 조인이 발생하므로 데이터가 예측할 수 없이 증가한다.`
  - 일대다에서 일(1)을 기준으로 페이징을 하는 것이 목적이다. 그런데 데이터는 다(**N**)를 기준으로 row 생성됨.
  - Order를 기준으로 페이징 하고 싶은데, 다(N)인 OrderItem을 조인하면 OrderItem이 기준이 되어버린다는 뜻.
- `하이버네이트는 경고 로그를 남기고 모든 DB 데이터를 읽어 메모리에서 페이징 시도, 최악의 경우 장애 발생.`

### 02-2. 한계 돌파

> 💡 페이징 + 컬렉션 엔티티를 함께 조회하려면 어떻게 해야 하는가?

```java
@Repository
@RequiredArgsConstructor
public class OrderRepository {
    
    private final EntityManager em;

    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        // 모든 엔티티에 fetch join을 적용하지 않는다.
        // 즉, member, delivery (ManyToOne, OneToOne) 관계만 fetch join, paging 적용
        // OrderItem, Item의 경우 지연 로딩으로 설정이 되어서, DTO 변환 시 Query 날라갈거임
        
        return em.createQuery(
                "select o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d", Order.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}
```

1. xToOne(ManyToOne, OneToOne) 관계를 모두 패치조인 한다.
   - 해당 연관관계로 설정된 엔티티의 데이터는 한 방에 땡겨 오라는 말이다.
2. 컬렉션은 `지연 로딩`으로 조회 한다.
3. 지연 로딩 성능 최적화를 위해 `hibernate.default_batch_fetch_size`, `@BatchSize 적용`.
   - **hibernate.default_batch_fetch_size** : 글로벌 설정
   - **@BatchSize** : 개별 최적화
   - 이 옵션 사용시 컬렉션, 프록시 객체를 한꺼번에 설정한 size 만큼 IN 쿼리로 조회.

### 02-3. 장점

1. 쿼리 호출 수가 `1 + N` -> `1 + 1` 로 최적화 된다. (IN query)
2. 조인보다 DB 데이터 전송량이 최적화 된다.
3. 페치 조인 방식과 비교해서 쿼리 호출 수가 약간 증가하지만, DB 데이터 전송량이 감소한다.
4. 컬렉션 페치 조인은 페이징이 불가능 하지만 이 방법은 페이징이 가능하다.

### 02-4. 결론

- xToOne 관계는 페치 조인해도 페이징에 영향을 주지 않음.
- xToOne 관계는 페치조인으로 쿼리 수를 줄이고 해결하고, 나머지는 hibernate.default_batch_fetch_size로 최적화.