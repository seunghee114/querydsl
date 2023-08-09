package com.study.querydsl.dto;
// 단점 : dto가 querydsl 라이브러리에 의존하게 됨
import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import org.springframework.data.jpa.repository.Query;

@Data
public class MemberTeamDto {
    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;

    @QueryProjection
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
