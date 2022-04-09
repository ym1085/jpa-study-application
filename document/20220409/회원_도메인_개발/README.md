```
@date   : 2022-04-09 16:41
@author : ymkim
@desc   : 회원 도메인 개발 & 회원 테스트 코드 작성 부분 정리
```

## 01. 회원 도메인 개발

### 구현 기능

- 회원 등록
- 회원 목록 조회

### 구현 순서

- 회원 엔티티 확인
- 회원 리포지토리 개발
- 회원 서비스 개발
- 회원 기능 테스트

### Member Repository 개발

```java
@Repository
class MemberRepository {
    
    // spring 에서는 @Autowired를 통해 주입도 지원 해준다 
    @PersistenceContext
    private EntityManager em;

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

- @PersistenceContext 어노테이션을 통해 bean 주입을 받는다.
- JPQL은 테이블 대상이 아닌 Entity 객체를 대상으로 질의를 한다.

```java
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
```

@Repository, @Service, @PersistenceContext 와 같은 어노테이션이 붙어 있으면 Spring에 의해 compnent scan을 진행하게 되고 애플리케이션 로딩 시점에 모든 bean(객체)이 전부 생성이 된다. 

### Member Service 개발

```java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    // 회원 가입
    @Transactional
    public Long save(Member member) {
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member) {
        // EXCEPTION : cnt || null
        // impl unique constraint to member id || other field
        List<Member> findMembers = memberRepository.findByName(member.getUsername());
        if (!findMembers.isEmpty()) {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        }
    }

    // 회원 전체 조회
    public List<Member> findAll(){
        return memberRepository.findAll();
    }

    // 회원 단건 조회
    public Member findById(Long id) {
        return memberRepository.findById(id);
    }
}
```

- 일단 기본적인 회원 서비스으 CRUD 기능은 위와 같이 구현이 되었다.

### @Transactional

- JPA의 어떤 모든 데이터 변경이나 로직들은 하나의 Transaction 안에서 수행이 되어야 한다.
- 지연 로딩 등의 기능을 사용하기 위함.
- 읽기에 `@Transactional(readOnly = true)` 옵션을 사용하여 성능 최적화 가능. 

### Spring Injection 관련

1. 필드 주입
2. setter 주입
3. 생성자 주입
4. 롬복 어노테이션 사용

**필드 주입**

```java
@Service
public class MemberService {
    @Autowired
    private MemberRepository memberRepository;
}
```

- 테스트 코드 작성 시 Mock 주입이 까다롭다. 

**setter 주입**

```java
@Service
public class MemberService {
    private MemberRepository memberRepository;
    
    public void setMemberRepository(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}
```

- 테스트 코드 작성 시 Mock 주입이 수월하다.
- 굳이 Setter를 만들어서 변경이 가능하게 만들 필요는 없음. 

**생성자 주입**

```java
@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }    
}
```

- 테스트 코드 작성 시 Mock 주입이 수월하다.
- Runtime 시점에 오류 파악이 가능.

**롬복 어노테이션 사용**

```java
@Service
// @AllArgsConstructor
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }    
}
```

- @AllArgsConstructor, `@RequiredArgsConstructor` 사용.
- @RequiredArgsConstructor 좀 더 생상적.
- 추후 차이점 확실하게 파악.

## 02. 회원 테스트 코드 작성

```java
/**
 * 회원 서비스 테스트 코드 작성
 *
 * @author ymkim
 * @since 2022.04.09 Sat 16:08
 *
 * @error
 *      No tests found for given includes
 *          - https://www.inflearn.com/questions/15495
 *          - https://ddasi-live.tistory.com/35
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired EntityManager em;

    @Test
    //@Rollback(false) -> 강제 롤백을 수행 안하려면
    public void 회원가입() throws Exception {
        //given
        Member member = new Member();
        member.setUsername("youngminkim");

        //when
        Long savedId = memberService.save(member);

        //then
        em.flush();
        assertEquals(member, memberRepository.findById(savedId));
    }

    @Test(expected = IllegalStateException.class)
    public void 중복_회원_예외() throws Exception {
        //given
        Member member1 = new Member();
        member1.setUsername("kim");

        Member member2 = new Member();
        member2.setUsername("kim");

        //when
        memberService.save(member1);
        memberService.save(member2); //예외 발생해야 함

        //then
        fail("예외가 발생해야 한다.");
    }
}
```

- `Junit4`를 사용해 테스트 진행 한다.
- 단위 테스트가 아닌 `통합 테스트`를 진행 한다.
- JPA는 같은 트랜잭션 안에서 같은 엔티티의 PK값이 같으면 하나의 영속성으로 관리가 된다. 
- 예외가 발생할 것 같으면 아래 구문을 추가하여 try - catch를 대체할 수 있다.

```java
@Test(expected = IllegalStateException.class)
```

### @RunWith(SpringRunner.class)

- `Junit` 실행할 때 `Spring`이랑 엮어서 실행하고 싶을 경우 사용이 된다.
- Junit 프레임워크의 테스트 실행 방법 확장 시 사용이 된다.
- 각각의 테스트 별도 객체가 생성 되더라도 싱클톤(Singleton)의 Application Context 보장.

### @SpringBootTest

- `통합 테스트`를 제공하는 기본적인 스프링 부트 테스트 어노테이션.
- 여러 단위의 테스트를 하나의 통합된 테스트로 수행할 때 사용되는 어노테이션.
- 모든 테스트 수행 가능.
- 애플리케이션에 설정된 모든 빈을 로드, 애플리케이션 성능 문제.

### @Transactional

- 테스트를 실행할 때 마다 트랜잭션을 시작하고, 테스트 완료 시 해당 트랜잭션 강제 롤백.
- `DB의 값`이 테스트 환경으로 인해 변경되지 않게 하기 위해 `강제 롤백`을 수행 한다.
- 테스트 코드 내에서만 롤백을 수행한다.

### 테스트 시 inmemory db 사용

```markdown
ㄴ test
    ㄴ java
    ㄴ resources
        ㄴ application.yml
```

- DB 뻗어도 Test 진행 가능.
- Spring Boot는 기본적으로 Inmemory DB 지원.

### 참고 자료

- [[문서] H2 Database](https://www.h2database.com/html/installation.html)
- [[문서] H2 Database Engine Cheat Sheet](https://www.h2database.com/html/cheatSheet.html)
- [[문서] Spring Test 관련 키워드 간단 정리](https://donghun.dev/Spring-Boot-Test-Keywrod-one)
- [[문서] Junit 개념, 특징, 어노테이션, 메서드](https://shlee0882.tistory.com/202)
- [[문서] TEST 코드 작성 시 오류 발생, Intellij 환경 셋팅](https://www.inflearn.com/questions/15495)