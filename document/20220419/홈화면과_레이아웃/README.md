```
@date   : 2022-04-19 20:46
@author : ymkim
@desc   : 웹 계층 개발
```

## 웹 계층 개발

### 목차

- 홈 화면과 레이아웃
- 회원 등록
- 회원 목록 조회
- 상품 등록
- 상품 목록
- 상품 수정
- 변경 감지와 병합(merge)
- 상품 주문
- 주문 목록 검색, 취소

### 기억해야 하는 부분

엔티티는 고유한 상태를 유지 하도록 설계하는 것이 좋다. 즉 이 말은 실무에서 요구사항이 단순하지 않기 때문에 
엔티티를 컨트롤러(Controller)에서 반환 하거나, Body로 받는 것은 `지양`해야 하는 부분이라는 말이다.  

데이터를 주고 받을 때는 DTO(Data Transfer Object) 객체를 사용하여 각각의 레이어 계층 간의 데이터를  
주고 받는 형식으로 도메인을 구성 하여야 한다. 이 때 엔티티와 DTO(Request, Response) 객체는 반드시  
같을 필요도 없고, 비즈니스의 복잡도에 따라서 같을 수 없을 확률이 높다.  

**REST API 설계 시 엔티티를 반환하면 안되는 이유**

- 컨트롤러의 반환 값으로 엔티티(Entity)를 사용하면 API 규격서가 일관되지 못할 가능성이 크다.
- 해당 Entity 값에 비밀번호(pwd)와 같은 개인 정보가 존재할 수 있음.
  - 해당 Entity -> DTO로 변환하여 필요한 데이터만 Model에 담아 반환 해야 한다.
- 비즈니스 초기 단계에서는 괜찮을수 있겠지만, 나중에는 피 눈물 흘릴수도 있음.

**API에서 어떻게 유효성 검증을 할 것인가?**

> 아래 코드는 이전 회사에서 인트라넷을 할 때 사용한 코드.

Spring Boot를 사용하기 이전 Spring을 사용할 때 아래와 같은 코드를 작성한 경험이 있다.

```java
import java.util.Map;

@Controller
@RequiredArgsConstructor
class MemberController {
    private final Logger log = LoggerFactory.getLogger(ApprovalController.class);
  
    private final MemberService memberService;
  
    @PostMapping("/member/join")
    public Model join(@RequestParam HashMap<String, Object> paramMap) {
        log.debug("paramMap = {}", paramMap.toString());
    
        String userId = paramMap.get("userId");
        String userPwd = paramMap.get("userPwd");
        String userName = paramMap.get("userName");
        String userPhoneNum = paramMap.get("userPhoneNum");
        String userAddress = paramMap.get("userAddress");
        String userAddressDetail = paramMap.get("userAddressDetail");
        // .... 중략 -> 필드가 더 있다 가정
    
        if (StringUtils.isNotBlank(userId)
                && StringUtils.isNotBlank(userPwd)
            /*  ...   */) {
            // 공란이 존재하지 않거나, 정규식에 맞는 경우 회원 가입 진행
            Map<String, Object> sqlParamMap = new HashMap<String, Object>();
            sqlParamMap.put("userId", userId);
            sqlParamMap.put("userPwd", userId);
            // ..중략
            memberService.join(sqlParamMap);
        }
    }
}
```

- 기존 Spring에서 위와 같이 유효성 검증을 했다.

```java
@Controller
@RequiredArgsConstructor
class MemberController {
    private final Logger log = LoggerFactory.getLogger(ApprovalController.class);
  
    private final MemberService memberService;
  
    @PostMapping("/member/join")
    public Model join(@Valid MemberForm memberForm, BindingResult result) {
        log.debug("memberForm = {}", memberForm.toString()); // DTO라 생각
      
        // 기존 레거시 StringUtils.isNotBlank 대체 -> BindingResult
        if (result.hasError()) {
            return "members/createMemberForm"; // 회원 가입 폼으로 다시 보내버리기
        }
        
        // map 같은거 안씀
        memberService.join(memberService);
    }
}
```

- 사람마다 케바케이겠지만, 훨씬 간단하게 유효성 검증을 처리할 수 있음.
- 화면(View) 한번 살펴보자.

```java
<div class="form-group">
    <label th:for="name">이름</label>
    <input type="text" th:field="*{name}" class="form-control" placeholder="이름을 입력하세요"
           th:class="${#fields.hasErrors('name')}? 'form-control fieldError' : 'form-control'">
    <p th:if="${#fields.hasErrors('name')}" th:errors="*{name}">Incorrect date</p>
</div>
```

- 위처럼 thymeleaf 에다가 hasErrors 써서 간단히 사용도 가능.