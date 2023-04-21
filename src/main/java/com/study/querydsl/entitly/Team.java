package com.study.querydsl.entitly;

import com.study.querydsl.entitly.Member;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
// jpa에서는 기본 생성자가 필요하기 때문에 기본 생성자를 protected level까지 허용
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "name"})
public class Team {
    @Id
    @GeneratedValue
    @Column(name = "team_id")
    private Long id;
    private String name;
    @OneToMany(mappedBy = "team")
    private List<Member> members = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }
}