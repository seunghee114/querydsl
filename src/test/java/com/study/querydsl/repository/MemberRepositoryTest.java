package com.study.querydsl.repository;

import com.study.querydsl.entitly.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

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
}
