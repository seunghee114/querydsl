package com.study.querydsl.repository;

import com.study.querydsl.entitly.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom{
                                    // 인터페이스 다중 상속 가능
    List<Member> findByUsername(String username);
}
