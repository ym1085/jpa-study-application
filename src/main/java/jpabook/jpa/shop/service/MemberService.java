package jpabook.jpa.shop.service;

import jpabook.jpa.shop.domain.Member;
import jpabook.jpa.shop.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 01. @Transactional   =>      javax || * spring
// 02. Injection        =>      field || setter || * constructor || annotation (@AllArgsConstructor, @RequiredArgsConstructor)

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;

    // 회원 가입
    @Transactional
    public Long save(Member member) {
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    // 회원 validation
    private void validateDuplicateMember(Member member) {
        List<Member> findMembers = memberRepository.findByName(member.getUsername());
        log.info("findMembers = {}", findMembers.toString());

        if (!findMembers.isEmpty()) {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        }
    }

    // 회원 전체 조회
    public List<Member> findAll(){
        return memberRepository.findAll();
    }

    // 회원 단건 조회
    public Member findById(Long id) {
        return memberRepository.findById(id);
    }

}
