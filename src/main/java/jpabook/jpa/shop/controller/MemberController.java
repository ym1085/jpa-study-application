package jpabook.jpa.shop.controller;

import jpabook.jpa.shop.domain.Address;
import jpabook.jpa.shop.domain.Member;
import jpabook.jpa.shop.form.MemberForm;
import jpabook.jpa.shop.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/members/join")
    public String createForm(Model model) {
        model.addAttribute("memberForm", new MemberForm());
        return "members/createMemberForm";
    }

    /**
     * 회원 전체 조회
     *  [Entity를 반환하면 안되는 이유]
     *  - Entity -> DTO로 변환하고 해당 DTO에서 원하는 데이터만 출력하는 방식으로 진행.
     *  - API 만들 때는 이유 불문하고 절대 Entity를 반환하면 안된다.
     *  - API 스펙이 변할 수 있는 문제와, 추가적인 문제가 발생할 수 있음.
     * @param model
     * @return
     */
    @GetMapping("/members")
    public String findAll(Model model) {
        List<Member> members = memberService.findAll();
        model.addAttribute("members", members);
        return "members/memberList";
    }

    @PostMapping("/members/join")
    public String save(@Valid MemberForm memberForm, BindingResult result) {
        log.info("memberForm = {}", memberForm.toString());

        if (result.hasErrors()) {
            return "members/createMemberForm";
        }

        Address address = new Address(memberForm.getCity(), memberForm.getStreet(), memberForm.getZipcode()); // 값 타입
        Member member = new Member();
        member.setUsername(memberForm.getName());
        member.setAddress(address);

        memberService.save(member);
        return "redirect:/";
    }
}
