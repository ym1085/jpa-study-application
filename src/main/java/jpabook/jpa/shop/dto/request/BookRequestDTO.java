package jpabook.jpa.shop.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
public class BookRequestDTO {

    private Long id;

    @NotBlank(message = "상품명은 필수 입력값 입니다.")
    private String name;

    @NotBlank(message = "가격은 필수 입력값 입니다.")
    private int price;

    @NotBlank(message = "재고 수량은 필수 입력값 입니다.")
    private int stockQuantity;

    @NotBlank(message = "저자명은 필수 입력값 입니다.")
    private String author;
    private String isbn;

}
