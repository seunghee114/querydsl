package com.study.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.dto.MemberDto;
import com.study.querydsl.dto.QMemberDto;
import com.study.querydsl.dto.UserDto;
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

    /**
     * 상수, 문자
     */
    @Test
    public void constant() {
        List<Tuple> result = queryFactory.select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    /**
     * 문자 더하기
     * {username}_{age}
     * age의 type이 숫자여서 concat하기가 어려운데 .stringValue()를 사용하면 문자로 바꿀 수 있다.
     * 문자가 아닌 다른 타입은 stringValue()로 문자로 바꿀 수 있다.
     * -> 해당 방법은 ENUM을 처리할 때도 자주 사용한다.
     */
    @Test
    public void concat() {
//        이건 안됨. 왜냐면 age의 type은 숫자니까
//        queryFactory.select(member.username.concat("_").concat(member.age))
        List<String> result = queryFactory.select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    /**
     * 프로젝션 대상이 하나인 경우
     */
    @Test
    public void simpleProjection(){
        List<String> result = queryFactory
                .select(member.username)    // 보통 여기에 들어가는 걸 projection이라고 한다.
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    /**
     * 프로젝션 대상이 여러 개인 경우
     */
    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username + " age = " + age);
        }
    }
    /**
     * tuple의 패키지를 보면 querydsl이다.
     * -> repository 객체에서 사용하는 것은 괜찮은데 service나 controller까지 넘어가는 것은 좋지 않은 것 같다.
     * -> 뭘 사용하는지 알려줄 필요가 없으니까!
     * -> 의존성이 없게 설계한 후에 나중에 querydsl이 아니라 다른 기술로 바꿔도 다른 계층은 영향을 받지 않도록
     */

    /**
     * 순수 JPA에서 DTO를 조회하는 방법
     * DTO의 패키지 이름을 다 적어주고, new 명령어를 사용해 생성
     * 생성자 방식만 지원
     */
//    @Test
//    public void findDtoByJPQL(){
//        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
//                .getResultList();
//    }

    /**
     * DTO로 프로젝션 결과 반환하는 방법
     * 1. 프로퍼티 접근 - setter
     * 2. 필드 직접 접근
     * 3. 생성자 사용
     */

    /**
     * 1. 프로퍼티 접근 - setter
     * getter setter 필요
     */
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    /**
     * 2. 필드 직접 접근
     * getter setter 없어도 필드에 직접 접근
     */
    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    /**
     * 3. 생성자 접근
     * dto 필드와 타입이 맞아야 한다.
     * 필드에 2개가 있는데 3개를 입력할 경우와 같은 문제를 런타임 시점에서 잡아준다.
     */
    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * UserDto로 조회할 때
     * username이 아니라 name이라는 필드를 가지고 있음
     * .as를 안해주면 매칭이 안되서 null로 들어감
     */
    @Test
    public void findUserDto() {
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result1 = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),

                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }
    @Test
    public void findUserDtoByConstructor() {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("memberDto = " + userDto);
        }
    }
    /**
     * @QueryProjection 사용
     * 필드에 2개가 있는데 3개를 입력할 경우와 같은 문제를 컴파일 시점에서 잡아준다.
     * 단점 : querydsl에 의존적인 설계가 되버림
     * dto는 repository, service, controller를 넘나들며 사용되는데 그런 dto안에 query projection이 들어있음
     * -> dto가 순수하지 않음
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 동적 쿼리 - boolean builder
     */
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }
    /**
     * 동적 쿼리 - where 다중 파라미터 사용
     * where 조건에서 null이면 무시함
     */
    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }
    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond): null;
    }
    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }
    // 조립 가능
    // 재사용 가능
    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * bulk 연산
     */
    @Test
    public void bulkUpdate() {
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        assertThat(count).isEqualTo(2);
        // bulk 연산은 1차캐시를 무시하고 db에 바로 해버리기 때문에 db의 값은 변경됨
        // 하지만 영속성 컨텍스트의 값은 변경되기 전의 값이 들어가 있다.
        // 즉, member1의 이름이 DB에는 비회원이지만 영속성컨텍스트는 member1임
        // db에서 온 값보다 영속성 컨텍스트의 값이 우선순위가 높다.
        // 그래서 아래의 코드의 결과는 db와 다른 값이 출력됨
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
        // 해결 방법 : 영속성 컨텍스트 초기화
        em.flush();
        em.clear();
        List<Member> result2 = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member1 : result2) {
            System.out.println("member1 = " + member1);
        }
    }
    // minus()는 없음. 따라서 add(-1)로!
    // 곱하기는 multiply(x)
    @Test
    public void bulkAdd() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }
    @Test
    public void bulkDelete(){
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFunction() {
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                                member.username, "member", "M"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 예제가 되게 별론데 기능을 보여주고 싶은 거
     * sql 문을 확인하자
     */
    @Test
    public void sqlFunction2(){
        List<String> fetch = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username)
//                ))
                .where(member.username.eq(member.username.lower()))
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }




}
