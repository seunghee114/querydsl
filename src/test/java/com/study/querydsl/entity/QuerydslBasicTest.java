package com.study.querydsl.entity;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.entitly.Member;
import com.study.querydsl.entitly.QMember;
import com.study.querydsl.entitly.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        // 여기서 만들면 중복 코드를 줄일 수 있다. Querydsl을 사용하는 메소드에서 매번 하지 않아도 된다.
        // 동시성 문제가 걱정될 수 있지만 Spring의 Entity Manaber가 멀티 스레드에서 문제없이 동작하도록 만들어져 있다.
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("temaA");
        Team teamB = new Team("temaB");
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
    }

    @Test
    public void startJPQL() {
        //member1을 찾아라.
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryds(){
        QMember m = new QMember("m");
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))    // 파라미터 바인딩
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

}
