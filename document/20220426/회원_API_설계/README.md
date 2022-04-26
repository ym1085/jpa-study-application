> 전체 회원 조회 API를 설계하던 중 나온 문제점

## 01. 응답 값으로 엔티티를 직접 외부에 노출한 경우

### 문제점

- 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다.
- 기본적으로 엔티티의 모든 값이 노출된다.
- 응답 스펙을 맞추기 위해 로직이 추가된다. (@JsonIgnore, 별도의 뷰 로직 등등)
- 실무에서는 같은 엔티티에 대해 API 용도에 따라 다양하게 만들어지는데, 한 엔티티에 각가의 API를 위한  
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