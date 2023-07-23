package com.study.querydsl.entity;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.entitly.Member;
import com.study.querydsl.entitly.QMember;
import com.study.querydsl.entitly.QTeam;
import com.study.querydsl.entitly.Team;
import org.assertj.core.api.Assertions;
import org.hibernate.boot.model.source.spi.EmbeddedAttributeMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static com.study.querydsl.entitly.QMember.member;
import static com.study.querydsl.entitly.QTeam.team;
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
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
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
    public void startQuerydsl(){
        // 지저분 버전
//        QMember m = new QMember("m");
//        QMember m1 = QMember.member;
//        Member findMember = queryFactory
//                .select(m)
//                .from(m)
//                .where(m.username.eq("member1"))    // 파라미터 바인딩
//                .fetchOne();

        //깔끔 버전
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10))
                .fetchOne();
        // 알아서 and로 조립됨
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        List<Member> fetch = queryFactory.selectFrom(member)
                            .fetch();

//        Member fetchOne = queryFactory.selectFrom(member)
//                            .fetchOne();
        // 여러 명인데 fetchOne()을 해서 오류남

        Member fetchFirst = queryFactory.selectFrom(member)
                            .fetchFirst();
    }

    /**
     * member 정렬
     * 1. 나이 내림차순
     * 2. 이름 오름차순
     * 2에서 회원 이름이 없으면 마지막에 출력
     * */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1(){
        List<Member> result = queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory.select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                ).from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    // 팀 이름과 각 팀의 평균 연령 구하기
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원
     * */
    @Test
    public void join(){
        List<Member> result = queryFactory.selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타조인 (연관관계가 없는 필드로 조인할 수 있음)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * 모든 멤버 테이블이랑 모든 팀 테이블을 조인 시켜서 팀이름이랑 멤버이름이 같은 거 가져오기
     * */

    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 회원과 팀을 조인하면서,
     * 팀 이름이 teamA인 팀만 조인
     * 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     * */
    @Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory.select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();
        for(Tuple tuple : result){
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 회원 외부조인
     * pk, fk 값 매칭이 없음
     * */

    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory.select(member, team)
                .from(member)
//                .leftJoin(member.team) id가 같은지 확인함
//                원래 조인은 이런 식으로 하는데 막 조인 할거라 그냥 team을 넣는다.
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for(Tuple tuple : result){
            System.out.println("tuple = " + tuple);
        }
    }
    @PersistenceUnit
    EntityManagerFactory emf;
    /**
     * 페치 조인 미적용
     */
    @Test
    public void fetchJoinUse(){
        // 영속성 컨텍스트를 DB에 반영 후, 영속성 컨텍스트를 날려 깔끔한 상태에서 시작
        em.flush();
        em.clear();

        // 멤버를 조회할 때 연관된 팀을 한꺼번에 가져온다.
        Member findMember = queryFactory.selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        // 파라미터로 받는 인자가 로딩이 되어있는지 안되어있는지 알려주는 함수
        // 페치 조인 적용 후이기 때문에 로딩이 됨.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("fetch join no").isTrue();
    }

    /**
     * 페치 조인 적용
     */
    @Test
    public void fetchJoinNo(){
        // 영속성 컨텍스트를 DB에 반영 후, 영속성 컨텍스트를 날려 깔끔한 상태에서 시작
        em.flush();
        em.clear();

        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // 파라미터로 받는 인자가 로딩이 되어있는지 안되어있는지 알려주는 함수
        // 페치 조인 적용 전이기 때문에 로딩이 되면 안됨.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("fetch join no").isFalse();
    }
    /**
     * 서브 쿼리 예시
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery(){
        // alias가 달라야 하니까 QMember를 하나 만들어서 서브 쿼리에 사용한다.
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age").containsExactly(40);
    }
    /**
     * 서브 쿼리 예시
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryGoe(){
        // alias가 달라야 하니까 QMember를 하나 만들어서 서브 쿼리에 사용한다.
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age").containsExactly(30, 40);
    }
    /**
     * 서브 쿼리 예시
     * 억지성 예제, in 쓰는 거 보여주기 위함
     */
    @Test
    public void subQueryIn(){
        // alias가 달라야 하니까 QMember를 하나 만들어서 서브 쿼리에 사용한다.
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    /**
     * select 서브 쿼리 예시
     */
    @Test
    public void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = queryFactory.select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member).fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.
     *
     * 해결방안
     * 1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고 불가능한 상황도 있음)
     * 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다. (상황에 따라 다르지만 2번 분리해서 실행하는 방법도 있음)
     * 3. native SQL을 사용한다.
     *
     * from 절에 서브쿼리를 사용하는 많은 이유가 있는데 대부분 안좋은 이유.
     * -> db는 데이터만 가져오는 용도로만 사용하고 application 단에서 로직을 처리하고 있는지 확인해보기
     */

    /**
     * 기본 case 구문
     */
    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    /**
     * 복잡한 case 구문
     */
    @Test
    public void complexCase() {
        List<String> result = queryFactory.select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0-20살")
                        .when(member.age.between(21, 30)).then("21-30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    /**
     * 꼭 case 문을 사용해야 하는가?
     * 이런 부분은 DB에서 하면 안됨. 좋지 않음
     * 그냥 가져온 다음에 application 단에서 로직으로 처리해주는 것이 더 좋음
     * (물론 DB에서 해야 할 경우도 있지만...)
     */










}
