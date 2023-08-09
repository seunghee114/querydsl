package com.study.querydsl.controller;

import com.study.querydsl.entitly.Member;
import com.study.querydsl.entitly.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {
    // spring boot를 실행하면 local의 application.yml로 실행된다.
    // 얘도 Profile이 local이기 때문에  얘가 실행이 된다.
    private final InitMemberService initMemberService;
    @PostConstruct
    public void init() {
        initMemberService.init();
    }
    @Component
    static class InitMemberService{
        @PersistenceContext
        private EntityManager em;
        // @PostConstruct와 @Transactional을 함께 사용할 수 없기 때문에 분리해줘야 한다.
        @Transactional
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");

            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectionTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member"+i, i, selectionTeam));
            }
        }
    }
}
