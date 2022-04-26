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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MemberAPIController {
    private final Logger log = LoggerFactory.getLogger(MemberAPIController.class);

    private final MemberService memberService;

    @GetMapping("/api/v1/members")
    public List<Member> membersV1() {
        // Member Entity 자체를 반환
        return memberService.findAll();
    }

    @GetMapping("/api/v2/members")
    public Result memberV2() {
        List<Member> findMembers = memberService.findAll();

        // Member Entity를 MemberDTO로 변환하여 반환
        // Member Entity를 그대로 반환하면 API 스펙이 변하고, 보안상 좋지 않음
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getUsername()))
                .collect(Collectors.toList());

        return new Result<>(collect.size(), collect);
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String username;
    }

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
