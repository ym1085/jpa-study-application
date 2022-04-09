//package jpabook.jpa.shop.member;
//
//import jpabook.jpa.shop.domain.Member;
//import jpabook.jpa.shop.domain.MemberRepository;
//import org.assertj.core.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.annotation.Rollback;
//import org.springframework.test.context.junit4.SpringRunner;
//import org.springframework.transaction.annotation.Transactional;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//public class MemberRepositoryTest {
//
//    @Autowired
//    MemberRepository memberRepository;
//
//    /**
//     * EntityManager에 대한 모든 변경은 하나의 Transaction 단위 안에서 수행 해야 함
//     *  1. Junit4 사용중
//     *  2. @Transactionals 어노테이션 추가
//     *  3. @Test 어노테이션에 Transaction 사용하면, Rollback 수행
//     *  4. Rollback 하기 싫으면 @Rollback(false) 사용
//     *
//     * @throws Exception
//     */
//    @Test
//    @Transactional
//    @Rollback(false)
//    public void testMember() throws Exception {
//        //given
//        Member member = new Member();
//        member.setUsername("ymkim");
//
//        //when
//        Long memberId = memberRepository.save(member);
//        Member findMember = memberRepository.find(memberId);
//
//        //then
//        Assertions.assertThat(findMember.getId()).isEqualTo(member.getId());
//        Assertions.assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
//        Assertions.assertThat(findMember).isEqualTo(member);
//        System.out.println("findMember == member : " + (findMember == member)); // 하나의 트랜잭션 단위에서 동일한 객체 반환 필요
//    }
//}