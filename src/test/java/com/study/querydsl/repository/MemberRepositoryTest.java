package com.study.querydsl.repository;

import com.study.querydsl.dto.MemberSearchCondition;
import com.study.querydsl.dto.MemberTeamDto;
import com.study.querydsl.entitly.Member;
import com.study.querydsl.entitly.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class MemberRepositoryTest {
    @Autowired
    EntityManager em;
    @Autowired
    MemberRepository memberRepository;
    @Test
    public void basicTest(){
        Member member = new Member("seunghee", 23);
        memberRepository.save(member);

        Member member1 = memberRepository.findById(member.getId()).get();
        assertThat(member1).isEqualTo(member);
        List<Member> result1 = memberRepository.findAll();
        assertThat(result1).containsExactly(member);
        List<Member> result = memberRepository.findByUsername("seunghee");
        assertThat(result).containsExactly(member);
    }
    @Test
    public void searchTest(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);


        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        // 조건이 아예 없을 경우에 쿼리가 데이터를 다 끌어온다. -> 데이터가 많을 경우 문제가 된다.
        condition.setTeamName("teamB");
        List<MemberTeamDto> result = memberRepository.search(condition);
        assertThat(result).extracting("username").containsExactly("member3", "member4");
    }
    @Test
    public void searchPageSimpleTest(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);


        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3);
        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result).extracting("username").containsExactly("member1","member2", "member3");
    }
}
