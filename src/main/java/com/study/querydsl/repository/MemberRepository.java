package com.study.querydsl.repository;

import com.study.querydsl.entitly.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {
                                    // 인터페이스 다중 상속 가능
                                                                    // QuerydslPredicateExecutor -> join 불가. 클라이언트가 querydsl에 의존
    List<Member> findByUsername(String username);
}
