package com.study.querydsl.repository;

import com.study.querydsl.entitly.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class MemberQdslRepositoryTest {
    @Autowired
    EntityManager em;
    @Autowired
    MemberQdslRepository memberQdslRepository;
    @Autowired
    MemberJpaRepository memberJpaRepository;
    @Test
    public void basicTest(){
        Member member = new Member("seunghee", 23);
        memberJpaRepository.save(member);

        List<Member> result1 = memberQdslRepository.findAll();
        assertThat(result1).containsExactly(member);
        List<Member> result = memberQdslRepository.findByUserName("seunghee");
        assertThat(result).containsExactly(member);
    }
}