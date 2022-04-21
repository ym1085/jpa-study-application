package jpabook.jpa.shop.form;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;

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
