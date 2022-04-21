```
@date   : 2022-04-21 20:41
@author : ymkim
@desc   : 변경 감지와 병합(merge)
```

## 01. 변경 감지와 병합(merge)

해당 내용은 JPA에서 중요도가 있는 부분이기 때문에 따로 정리를 하였습니다.

> 정말 중요한 부분, 완벽하게 이해를 해야 함  
> 키워드는 `변경 감지`, `병합` 이다

### 01-1. 준영속 엔티티?

```java
@Getter @Setter
@ToString
public class BookForm {

    private Long id;

    @NotEmpty(message = "상품명은 필수 입니다.")
    private String name;

    private int price;
    private int stockQuantity;

    private String author;
    private String isbn;
}
```

- Form 데이터를 받기 위해 생성한 클래스
- DTO로 사용이 되는 부분을 위와 같이 표현

```java
@Controller
@RequiredArguments
public class ItemController {
    //...

    @PostMapping("/items/{itemId}/edit")
    public String updateItem(@PathVariable String itemId, @ModelAttribute("form") BookForm form) {
        log.debug("form = {}", form.toString());
        Book book = new Book();     // 새로운 객체
        book.setId(form.getId());   // ID 셋팅 -> DB에 들어갔다가 나온 상태
        book.setName(form.getName());
        book.setPrice(form.getPrice());
        book.setStockQuantity(form.getStockQuantity());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());
  
        itemService.save(book);
        return "redirect:/items";
    }
}
```

- **영속성 컨텍스트에 의해 더 이상 관리가 되지 않는 엔티티**를 `준영속 엔티티`라 지칭한다.  
- 여기서는 itemService.save(book) 에서 수정을 시도하는 `Book 객체`다.
  - Book 객체는 이미 DB에 한 번 저장이 되어 식별자가 존재.
- 이렇게 임의로 만들어낸 엔티티(Book 객체)도 기존 식별자를 가지고 있으면 준영속 엔티티로 볼 수 있다.
  - 준영속 엔티티는 JPA에 의해 관리가 되지 않는다.
  - 그렇다면 이러한 준영속 엔티티의 데이터는 어떻게 변경을 해야 하는가?

### 01-2. 준영속 엔티티를 수정하는 2가지 방법

- `변경 감지` 기능을 사용
  - dirty checking
- `병합`(merge) 사용

### 변경 감지 기능 사용

```java
// 전체
@Service
public class ItemService {
    
    //...
    
    @Transactional
    public void updateItem(Long itemId, Book param) {
        Item findItem = itemRepository.findById(itemId);
        findItem.setPrice(param.getPrice());
        findItem.setName(param.getName());
        findItem.setStockQuantity(param.getStockQuantity());
        // Transaction commit 시점에 변경 상태가 확인이 된다  
    }    
}
```

- 변경 감지에 의해 값을 변경하는 예제
- 웬만하면 단발성 setter를 사용하지말고, 의미있는 메서드를 작성하는 것이 좋다

### merge 사용(영속 엔티티 -> 준영속 엔티티의 데이터로 바꿔치기)

```java
public void save(Item item) {
    if (item.getId() == null) {
        em.persist(item);
    } else {
        em.merge(item); // id 값이 존재하면, update
    }
}
```

1. 준영속 엔티티의 식별자 값으로 영속 엔티티를 조회한다.
2. 영속 엔티티의 값을 준영속 엔티티의 값으로 모두 교체한다. (병합)
3. 트랜잭션 커밋 시점에 변경 감지 기능이 동작해서 DB에 UPDATE SQL이 실행 됨.

> 주의 : 변경 감지 기능을 사용하면 원하는 속성만 선택해서 변경할 수 있지만, 병합을 사용하면 
> 모든 속성이 변경된다.  
> 즉, 이 말은 병합(merge)시 준영속 엔티티의 값이 없으면 Null로 업데이트 할 위험이 존재.

### 가장 좋은 해결 방법

- 실무에서는 merge를 사용하는 것이 아니라, 변경 감지 기능을 사용하여 로직을 작성해야 한다
- 즉, `merge` 사용은 지양하자

### 엔티티를 변경 할때는 변경 감지를 사용하자

- 컨트롤러에서 어설프게 엔티티를 생성하지 말자

```java
@Controller
@RequiredArgsConstruct
public class ItemController {
    
    //....
  
    @PostMapping("/items/{itemId}/edit")
    public String updateItem(@PathVariable Long id, @ModelAttribute("form") BookForm form) {
        Book book = new Book(); // 어설프게 엔티티 생성
        book.setPrice(form.getPrice());
        book.setName(form.getName());
        //.. 중략
      
        itemService.save(book);
    }
}
```

- 트랜잭션이 있는 서비스 계층에 식별자(`id`)와 변경할 데이터를 명확하게 전달하자(`parameter or dto`)
- 트래잭션이 있는 서비스 계층에서 영속 상태의 엔티티를 조회하고, 엔티티의 데이터를 직접 변경하자(`변경 감지`)
- `트랜잭션 커밋 시점에 변경 감지가 실행된다`

