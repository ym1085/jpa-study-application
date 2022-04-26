package jpabook.jpa.shop.api;

import jpabook.jpa.shop.domain.Member;
import jpabook.jpa.shop.service.MemberService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class MemberAPIController {
    private final Logger log = LoggerFactory.getLogger(MemberAPIController.class);

    private final MemberService memberService;

    // @RequestBody --> JSON -> Member
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        log.info("member = {}", member);
        Long id = memberService.save(member);
        return new CreateMemberResponse(id);
    }

    /**
     * API specification does not change even if entity is changed
     *
     * @param request
     * @return
     */
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        log.info("request = {}", request);

        Member member = new Member();
        member.setUsername(request.getUsername());

        Long id = memberService.save(member);
        return new CreateMemberResponse(id);
    }

    @Data
    static class CreateMemberRequest {
        private String username;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }
}
