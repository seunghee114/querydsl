package com.study.querydsl.entitly;

import lombok.*;

import javax.persistence.*;
// jpa에서는 기본 생성자가 필요하기 때문에 기본 생성자를 protected level까지 허용
// toString에 team을 넣으면 안된다. 넣으면 team에서 member에 접근하고 이러니까 무한 루프에 빠질 수 있다.

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"})
public class Member {
    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public Member(String username) {
        this(username, 0);
    }

    public Member(String username, int age) {
        this(username, age, null);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}