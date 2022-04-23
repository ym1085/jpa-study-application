package jpabook.jpa.shop.controller;

import jpabook.jpa.shop.domain.Member;
import jpabook.jpa.shop.domain.Order;
import jpabook.jpa.shop.domain.item.Item;
import jpabook.jpa.shop.repository.OrderSearch;
import jpabook.jpa.shop.service.ItemService;
import jpabook.jpa.shop.service.MemberService;
import jpabook.jpa.shop.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final MemberService memberService;
    private final ItemService itemService;

    @GetMapping("/order")
    public String createForm(Model model) {
        List<Member> members = memberService.findAll();
        List<Item> items = itemService.findAll();
        model.addAttribute("members", members);
        model.addAttribute("items", items);
        return "order/orderForm";
    }

    /**
     * 상품 주문
     *
     * @param memberId
     * @param itemId
     * @param count
     * @return
     */
    @PostMapping("/order")
    public String order(@RequestParam("memberId") Long memberId,
                        @RequestParam("itemId") Long itemId,
                        @RequestParam("count") int count) {

        orderService.order(memberId, itemId, count);
        return "redirect:/orders";
    }

    /**
     * 상품 검색
     *
     * @param orderSearch
     * @param model
     * @return
     */
    @GetMapping("/orders")
    public String findAll(@ModelAttribute("orderSearch") OrderSearch orderSearch, Model model) {
        log.info("orderSearch = {}", orderSearch.toString());
        List<Order> orders = orderService.searchOrder(orderSearch);
        model.addAttribute("orders", orders);
        return "order/orderList";
    }

    @PostMapping("/orders/{orderId}/cancel")
    public String cancelOrder(@PathVariable("orderId") Long orderId) {
        orderService.cancelOrder(orderId);
        return "redirect:/orders";
    }
}
