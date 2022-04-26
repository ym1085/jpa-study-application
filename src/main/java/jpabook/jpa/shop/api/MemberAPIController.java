package jpabook.jpa.shop.api;

import jpabook.jpa.shop.domain.Member;
import jpabook.jpa.shop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

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
        member.setUsername(request.getUsername()); // 추후 -> toEntity 사용

        Long id = memberService.save(member);
        return new CreateMemberResponse(id);
    }

    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdateMemberRequest request) {

        // command와 query 분리
        memberService.update(id, request.getUsername());

        Member findMember = memberService.findById(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getUsername());
    }

    @Data
    static class UpdateMemberRequest {
        private String username;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String username;
    }

    @Data
    static class CreateMemberRequest {
        @NotEmpty
        private String username;
    }

    @Data
    @AllArgsConstructor
    static class CreateMemberResponse {
        private Long id;
    }
}
