package com.study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {
    private String username;
    private int age;
    // @QueryProjection 어노테이션이 달려있으면 Q타입 객체가 생성됨
    // 사용한 후 querydsl을 한 번 compile 해줘야 한다.
    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
