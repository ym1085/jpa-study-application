package jpabook.jpa.shop.service;

import jpabook.jpa.shop.domain.Member;
import jpabook.jpa.shop.repository.MemberRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;

/**
 * 회원 서비스 테스트 코드 작성
 *
 * @author ymkim
 * @since 2022.04.09 Sat 16:08
 *
 * @error
 *      No tests found for given includes
 *          - https://www.inflearn.com/questions/15495
 *          - https://ddasi-live.tistory.com/35
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired EntityManager em;

    @Test
    //@Rollback(false)
    public void 회원가입() throws Exception {
        //given
        Member member = new Member();
        member.setUsername("youngminkim");

        //when
        Long savedId = memberService.save(member);

        //then
        em.flush();
        assertEquals(member, memberRepository.findById(savedId));
    }

    @Test(expected = IllegalStateException.class)
    public void 중복_회원_예외() throws Exception {
        //given
        Member member1 = new Member();
        member1.setUsername("kim");

        Member member2 = new Member();
        member2.setUsername("kim");

        //when
        memberService.save(member1);
        memberService.save(member2); //예외 발생해야 함

        //then
        fail("예외가 발생해야 한다.");
    }
}