```
@date   : 2022-04-03 18:06
@author : ymkim
@desc   : 엔티티 설계 관련 체크 사항 간단히 정리
```

## Getter & Setter

실무에서 Entity 설계를 하는 경우 웬만하면 Getter는 열어두고 Setter는 제공을 하지 않는 것이 좋다.  
즉, Setter를 사용하기 위한 별도의 메서드를 제공하는 것이 유지보수 차원에서 효율적이다.

## @ManyToMany

```java
// 즉시 로딩은 n + 1의 문제를 발생 시키기에, 지연 로딩으로 설정 해야 함
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn
private Team team;
```

실무에서는 가급적이면 @ManyToMany 어노테이션을 사용하지 말자. 또한 OneToOne, ManyToOne은 기본값이 즉시 로딩을 사용하기  
때문에, 해당 어노테이션을 사용하는 경우 패치 타입을 LAZY로 설정 해줘야 한다.

## 값 타입 - 임베디드 타입 설정 시

```java
@Embeddable
@Getter
public class Address {
    private String city;
    private String street;
    private String zipcode;

    // JPA 기본 스펙상 기본 생성자 하나를 생성 
    protected Address() {
    }

    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}
```

현재 Address는 값 타입(임베디드 타입)이다. 값 타입은 Setter를 제공하지 않고 생성할 때만 값이 셋팅이 되고  
다른 방식으로는 생성이 불가능 하도록 구조를 잡아야 한다. 또한 JPA 기본 스펙에 의해 기본 생성자가 필요함.

> JPA 스펙상 엔티티나 임베디드 타입(@Embeddable) 은 자바 기본 생성자를 public 또는 protected로 설정해야 한다.  
> JPA가 이러한 제약을 두는 이뉴는 JPA 구현 라이브러리가 객체를 생성할 때 리플랙션 같은 기술을 사용할 수 있도록 지원해야 하기 때문이다.