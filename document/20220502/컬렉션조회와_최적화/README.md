## API 개발 고급 - 컬렉션 조회 최적화

```java
@OneToMany(mappedBy = "member")
private List<Order> orders = new ArrayList<Order>();
```

주문 내역에서 추가로 주문한 상품 정보를 추가로 조회하자.  
Order 기준으로 컬렉션인 OrderItem와 Item이 필요하다.

앞의 예제에서는 xToOne(OneToOne, ManyToOne) 관계만 있었다.  
이번에는 컬렉션인 일대다 관계(OneToMany)를 조회하고, 최적화 하는 방법을 알아보자

