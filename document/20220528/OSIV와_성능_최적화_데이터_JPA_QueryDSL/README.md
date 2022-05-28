## 01. OSIV와 성능 최적화

- **Open Session In View**: 하이버네이트
- Open EntityManager In View: JPA
  - 관례상 OSIV라 한다.

### 01-1. OSIV ON

```
`spring.jpa.open-in-view`: 기본값은 true로 설정이 되어있다.
```

`JPA의 영속성 컨텍스트는 기본적으로 트랜잭션이 시작되는 순간에 DB Connection을 얻어 작업을 수행 한다.`

`OSIV(Open Session In View) 전략은 트랜잭션 시작처럼 최초 DB Connection 시점부터 API 응답이 끝날 때까지 영속성 
컨텍스트와 DB Connection을 유지한다`. 즉, 트랜잭션이 끝나도 영속성 컨텍스트를 끝까지 살려두는 것을 의미한다. 그래서 지금까지 
View Template이나 API 컨트롤러에서 지연 로딩이 가능 했던 것이다.

`지연 로딩`은 영속성 컨텍스트가 살아있어야 가능하고, 영속성 컨텍스트는 기본적으로 DB Connection을 유지한다. 이것 자체가 큰 장점. 
하지만 해당 전략(OSIV)은 너무 오랜 시간동안 DB Connection Resource를 사용하기 때문에, 실시간 트래픽이 중요한 Application
에서는 커넥션이 모자를 수 있으며, 이러한 이유가 장애로 이어지는 경우가 발생 한다.

> 단점 : 컨트롤러에서 외부 API를 호출하면 외부 API 대기 시간 만큼 커넥션 리소스를 반환 못하고, 유지해야 한다.  
> 단점 : DB Connection을 너무 오래 물고 있어서, 리소스 낭비 혹은 장애로 연결

### 01-2. OSIV OFF

```
`spring.jpa.open-in-view`: OSIV 종료
```

```yml
jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        format_sql: true
        default_batch_fetch_size: 100 # default_batch_fetch_size -> 미리 데이터를 가져와서 IN query를 날린다
    open-in-view: false # OSIV 속성은 false로 설정
```

OSIV를 끄면 트랜잭션을 종료할 때 `영속성 컨텍스트를 닫고`, `DB Connection도 반환` 한다. `따라서 커넥션 리소스 
를 낭비하지 않는다`. OSIV를 끄면 모든 지연로딩을 트랜잭션 안에서 처리해야 한다. 따라서 지금까지 작성한 많은 지연 로딩 
코드를 `트랜잭션 안`으로 넣어야 한다는 단점이 있다. 또한 view template에서 지연로딩이 동작하지 않는다. 결론적으로 
트랜잭션이 끝나기 전에 지연 로딩을 강제로 호출해 두어야 한다. 즉, 기본에 사용한 지연 로딩 관련된 내용을 서비스 레이어 
영역으로 옮기거나, fetch join을 사용하여 문제를 해결 하여야 한다. 

### 01-3. 커멘드와 쿼리 분리

실무에서 OSIV를 끈 상태로 복잡성을 관리하는 방법은 Command와 Query를 분리하는 것이다.

보통 비즈니스 로직은 특정 엔티티 몇 개를 등록하거나 수정하는 것이므로 성능이 크게 문제가 되지 않는다. 하지만 복잡한 화면을 
출력하기 위한 쿼리는 화면에 맞추어 성능을 최적화 하는것이 중요하다. 하지만 그 복잡성에 비해 핵심 비즈니스에 큰 영향을 주는 
것은 아니다. `그래서 크고 복잡한 애플리케이션을 개발한다면, 이 둘의 관심사를 명확하게 분리하는 것이 좋다.`
  
- OrderService
  - OrderService : 핵심 비즈니스 로직
  - OrderQueryService : 화면이나 API에 맞춘 서비스 (주로 읽기 전용 트랜잭션 사용)

보통 서비스 계층에서 트랜잭션을 유지, 두 서비스 모두 트랜잭션을 유지하면서 지연 로딩 사용이 가능하다.

### 01-4. 결론

- `고객 서비스의 실시간 API`는 OSIV를 끄고, `ADMIN`처럼 커넥션을 많이 사용하지 않는 곳에서는 OSIV를 켠다.
  - 위 내용이 무조건 정답은 아니다.
- 기본적으로 OSIV를 켜는 것이 좋지만, 성능 이슈가 있을 수 있기 때문에 도메인에 맞는 선택을 해야 한다.

## 02. Spring Data JPA 소개

> Spring Data JPA의 핵심은 기존 코드의 중복 제거

Spring Data JPA는 JPA 사용시에 지루하게 반복되는 코드를 자동화 해준다.

### 02-1. build.gradle

```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```

- 해당 라이브러리를 build.gradle에 입력하여 사용 한다.

```java
// AS-IS : MemberRepository
@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final EntityManager em;

    public void save(Member member) {
        em.persist(member);
    }

    public Member findById(Long id) {
        return em.find(Member.class, id);
    }

    public List<Member> findAll() {
        // target : Member entity
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findByName(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }
}
```

현재 Spring Data JPA를 적용하기 전의 MemberRepository의 코드는 위와 같다.  
위와 같은 내용을 Spring Data JPA로 Converting 하는 작업을 진행 해보자.

```java
// TO-BE : MemberRepository
public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByUsername(String username);
    
    //.. 중략
}
```

- findOne() -> findById() 로 변경 해야 한다.
- 기존 Repository를 Interface MemberRepository로 변경 하고 JpaRepository<T, type> 을 상속 받는다.

### 02-2. 결론

Spring Data JPA를 사용하면 반복적인 코드의 제거가 가능하지만, 기본적인 JPA 메커니즘을 이해하는 것이 더 중요하다.

## 03. QueryDSL

- `JPQL`을 Java 소스를 작성할 수 있게 해주는 Open Source 라이브러리.
- 대표적으로 Criteria 존재.
- 실무에서는 조건에 따라 실행되는 쿼리가 달라지는 `동적 쿼리`를 많이 사용

### 03-1. QueryDSL 설정

- 버전마다 QueryDSL 설정은 달라짐.

```groovy
//querydsl 추가
//issue: https://www.inflearn.com/questions/149157
buildscript {
	dependencies {
		classpath "gradle.plugin.com.ewerk.gradle.plugins:querydsl-plugin:1.0.10"
	}
}

plugins {
	id 'org.springframework.boot' version '2.4.1'
	id 'io.spring.dependency-management' version '1.0.11.RELEASE'
	id 'java'
}

group = 'jpabook'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

//apply plugin: 'io.spring.dependency-management'
apply plugin: "com.ewerk.gradle.plugins.querydsl"

configurations {
	compileOnly {
		extendsFrom annotationProcessor
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
	implementation 'org.springframework.boot:spring-boot-starter-web'
	implementation 'org.springframework.boot:spring-boot-devtools'
	implementation 'org.springframework.boot:spring-boot-starter-validation'
	implementation 'junit:junit:4.13.1'
	implementation ("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.5.6")
	implementation 'com.fasterxml.jackson.datatype:jackson-datatype-hibernate5'

	compileOnly 'org.projectlombok:lombok'
	runtimeOnly 'com.h2database:h2'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'

	implementation 'com.querydsl:querydsl-jpa'
	implementation 'com.querydsl:querydsl-apt'
}

tasks.named('test') {
	useJUnitPlatform()
}

//querydsl 추가
//def querydslDir = 'src/main/generated'
def querydslDir = "$buildDir/generated/querydsl"

querydsl {
	library = "com.querydsl:querydsl-apt"
	jpa = true
	querydslSourcesDir = querydslDir
}

sourceSets {
	main {
		java {
			srcDirs = ['src/main/java', querydslDir]
		}
	}
}

compileQuerydsl{
	options.annotationProcessorPath = configurations.querydsl
}

configurations {
	querydsl.extendsFrom compileClasspath
}
```

### 03-2. QueryDSL 예제

```java
import jpabook.jpa.shop.domain.QMember;
import jpabook.jpa.shop.domain.QOrder;

/**
 * 20220528 QueryDSL 사용
 *
 * @param orderSearch
 * @return
 */
public List<Order> findAll(OrderSearch orderSearch) {
    JPAQueryFactory query = new JPAQueryFactory(em);

    return query
            .select(order)
            .from(order)
            .join(order.member, member)
            .where(statusEq(orderSearch.getOrderStatus()), nameLike(orderSearch.getMemberName()))
            .limit(1000)
            .fetch();
}

private BooleanExpression nameLike(String memberName) {
    if (!StringUtils.hasText(memberName)) {
        return null;
    }
    return member.username.like(memberName);
}

private BooleanExpression statusEq(OrderStatus statusCondition) {
    if (statusCondition == null) {
        return null;
    }
    return order.status.eq(statusCondition);
}
```

`QueryDSL`

- 기존 JPQL 코드가 코드 몇줄로 변경 가능.
- 직관적인 문법.
- 컴파일 시점에 문법 오류 발견 가능.
- 코드 자동완성 지원.