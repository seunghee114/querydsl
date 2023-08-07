package com.study.querydsl.repository;

import com.study.querydsl.entitly.Member;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {
    @Autowired
    EntityManager em;
    @Autowired
    MemberJpaRepository memberJpaRepository;
    @Test
    public void basicTest(){
        Member member = new Member("seunghee", 23);
        memberJpaRepository.save(member);

        Member member1 = memberJpaRepository.findById(member.getId()).get();
        assertThat(member1).isEqualTo(member);
        List<Member> result1 = memberJpaRepository.findAll();
        assertThat(result1).containsExactly(member);
        List<Member> result = memberJpaRepository.findByUserName("seunghee");
        assertThat(result).containsExactly(member);
    }

}