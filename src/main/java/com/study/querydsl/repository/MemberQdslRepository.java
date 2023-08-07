package com.study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.entitly.Member;
import com.study.querydsl.entitly.QMember;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

import static com.study.querydsl.entitly.QMember.*;

@Repository
public class MemberQdslRepository {
//    JPAQueryFactory의 동시성 문제는 EntityManger에 의존
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

//        쿼리 팩토리를 spring bean으로 등록해도 된다.
//        해당 방법은 Application 클래스에서 아래를 추가
//    @Bean
//    JPAQueryFactory jpaQueryFactory(EntityManager em) {
//        return new JPAQueryFactory(em);
//    }
//    그 다음 이렇게. 이 방법이 편한 이유는 lombock에 있는 requiredArgumentconstructor 어노테이션 사용 가능
//    public MemberQdslRepository(EntityManager em, JPAQueryFactory queryFactory) {
//        this.em = em;
//        this.queryFactory = queryFactory;
//    }

    public MemberQdslRepository(EntityManager em) {
//        순수 JPA이기 때문에 Entity에 접근할 때 EntityManger가 필요
//        queryDSL을 사용하기 위해 JPAQueryFactory가 필요
//        얘는 또 entityManger가 필요
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }
    public List<Member> findAll(){
        return queryFactory
                .selectFrom(member)
                .fetch();
    }
    public List<Member> findByUserName(String username){
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }
}
