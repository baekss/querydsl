package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
class QuerydslBasicTest {

	@Autowired
	EntityManager em;
	
	JPAQueryFactory queryFactory;
	
	@BeforeEach
	public void before() {
		queryFactory = new JPAQueryFactory(em); //thread safe 함
		
		Team teamA = new Team("오");
		Team teamB = new Team("위");
		em.persist(teamA);
		em.persist(teamB);
		
		Member member1 = new Member("여몽", 40, teamA);
		Member member2 = new Member("육손", 20, teamA);
		Member member3 = new Member("장합", 42, teamB);
		Member member4 = new Member("학소", 22, teamB);
		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);
	}

	@Test
	public void startJPQL() {
		//여몽을 찾아라.
		Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
					.setParameter("username", "여몽")
					.getSingleResult();
		
		assertThat(findMember.getUsername()).isEqualTo("여몽");
	}
	
	@Test
	public void startQuerydsl() {
		//QMember m = new QMember("m"); //JPQL Member의 별칭 "m" self join시 별칭 다르게 해야할때 사용
		//QMember m = QMember.member;
		String username = "학소";
		
		Member findMember = queryFactory
							.select(member)
							.from(member)
							.where(member.username.eq(username))
							.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("학소");
	}
	
	@Test
	public void search() {
		String username = "장합";
		Member findMember = queryFactory
							.selectFrom(member)
							.where(member.username.eq(username)
									.and(member.age.gt(40)))
							.fetchOne();
		
		assertThat(findMember.getUsername()).isEqualTo("장합");
	}
	
	@Test
	public void searchAndParam() {
		String username = "장합";
		Member findMember = queryFactory
							.selectFrom(member)
							.where(member.username.eq(username), member.age.gt(40)) //가변인자값은 AND절로 풀린다.
							.fetchOne();
		
		assertThat(findMember.getUsername()).isEqualTo("장합");
	}
	
	@Test
	public void resultFetch() {
		//리스트 조회, 데이터 없으면 empty list 반환
		List<Member> fetch = queryFactory
							.selectFrom(member)
							.fetch();
		
		//단건 데이터 조회, 결과 없으면 null, 결과 다건이면 NonUniqueResultException
		/*
		Member fetchOne = queryFactory
						.selectFrom(member)
						.fetchOne();
		*/
		
		//첫번째 데이터(limit(1)) 조회
		Member fetchFirst = queryFactory
							.selectFrom(member)
							.fetchFirst();
		
		//count 쿼리와 select 쿼리 두개를 각각 실행
		QueryResults<Member> results = queryFactory
									.selectFrom(member)
									.fetchResults();
		
		long total = results.getTotal(); //count
		List<Member> content = results.getResults(); //select
		
		content.stream().forEach(m -> System.out.println("total : "+total+" member : "+m));
		
		//count 쿼리로 변경, count(entity) 일 땐 count(entity.id)로 됨
		long count = queryFactory
					.selectFrom(member)
					.fetchCount();
	}
	
	/**
	 * 회원 정렬 순서
	 * 1. 회원 나이 내림차순(desc)
	 * 2. 회원 이름 오름차순(asc)
	 * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
	 */
	@Test
	public void sort() {
		em.persist(new Member(null, 22));
		/**
		select
	        member1 
	    from
	        Member member1 
	    where
	        member1.age >= ?1 
	    order by
	        member1.age desc,
	        member1.username asc nulls last
		 */
		List<Member> result = queryFactory
							.selectFrom(member)
							.where(member.age.goe(20))
							.orderBy(member.age.desc(), member.username.asc().nullsLast())
							.fetch();
		
		result.stream().forEach(System.out::println);
		/**
		Member(id=5, username=장합, age=42)
		Member(id=3, username=여몽, age=40)
		Member(id=6, username=학소, age=22)
		Member(id=7, username=null, age=22)
		Member(id=4, username=육손, age=20)
		*/
	}
	
	@Test
	public void paging1() {
		List<Member> result = queryFactory
							.selectFrom(member)
							.orderBy(member.username.desc())
							.offset(1)
							.limit(2)
							.fetch();
		
		result.stream().forEach(System.out::println);
		/**
		Member(id=5, username=장합, age=42)
		Member(id=4, username=육손, age=20)
		*/
	}
	
	@Test
	public void paging2() {
		QueryResults<Member> queryResults = queryFactory
									.selectFrom(member)
									.orderBy(member.username.desc())
									.offset(1)
									.limit(2)
									.fetchResults();
		
		assertThat(queryResults.getTotal()).isEqualTo(4);
		assertThat(queryResults.getLimit()).isEqualTo(2);
		assertThat(queryResults.getOffset()).isEqualTo(1);
		assertThat(queryResults.getResults().size()).isEqualTo(2);
	}
	
	@Test
	public void aggregation() {
		List<Tuple> result = queryFactory
						.select(
								member.count(),
								member.age.sum(),
								member.age.avg(),
								member.age.max(),
								member.age.min()
						)
						.from(member)
						.fetch();
		
		Tuple tuple = result.get(0);
		assertThat(tuple.get(member.count())).isEqualTo(4);
		assertThat(tuple.get(member.age.sum())).isEqualTo(124);
		assertThat(tuple.get(member.age.avg())).isEqualTo(31);
		assertThat(tuple.get(member.age.max())).isEqualTo(42);
		assertThat(tuple.get(member.age.min())).isEqualTo(20);
	}
	
	/**
	 * 팀의 이름과 각 팀의 평균 연령을 구해라.
	 */
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
		
		assertThat(teamA.get(team.name)).isEqualTo("오");
		assertThat(teamA.get(member.age.avg())).isEqualTo(30);
		
		assertThat(teamB.get(team.name)).isEqualTo("위");
		assertThat(teamB.get(member.age.avg())).isEqualTo(32);
	}
	
	/**
	 * 오나라 장수
	 */
	@Test
	public void join() {
		List<Member> result = queryFactory
						.selectFrom(member)
						.innerJoin(member.team, team)
						.where(team.name.eq("오"))
						.fetch();
		
		assertThat(result)
				.extracting("username")
				.containsExactly("여몽", "육손");
	}
	
	/**
	 * 세타 조인(연관관계 없어도 join 가능)
	 * 회원의 이름이 팀 이름과 같은 회원 조회
	 */
	@Test
	public void thetaJoin() {
		em.persist(new Member("오"));
		em.persist(new Member("위"));
		em.persist(new Member("촉"));
		//cross join
		/**
		select
            member0_.member_id as member_i1_0_,
            member0_.age as age2_0_,
            member0_.team_id as team_id4_0_,
            member0_.username as username3_0_ 
        from
            member member0_ cross 
        join
            team team1_ 
        where
            member0_.username=team1_.name
		 */
		List<Member> result = queryFactory
						.select(member)
						.from(member, team)
						.where(member.username.eq(team.name))
						.fetch();
		
		assertThat(result)
				.extracting("username")
				.containsExactly("오", "위");
	}
	
	/**
	 * 예) 회원과 팀을 조인하면서, 팀 이름이 '위'인 팀만 조인, 회원은 모두 조회 
	 * JPQL : select m, t from Member m left join m.team t on t.name = '위'
	 */
	@Test
	public void joinOnFiltering() {
		/**
		select
	        member1,
	        team 
	    from
	        Member member1   
	    left join
	        member1.team as team with team.name = ?1
		 */
		/*
		select
            member0_.member_id as member_i1_0_0_,
            team1_.team_id as team_id1_1_1_,
            member0_.age as age2_0_0_,
            member0_.team_id as team_id4_0_0_,
            member0_.username as username3_0_0_,
            team1_.name as name2_1_1_ 
        from
            member member0_ 
        left outer join
            team team1_ 
                on member0_.team_id=team1_.team_id 
                and (
                    team1_.name=?
                )
		 */
		List<Tuple> result = queryFactory
				.select(member, team)
				.from(member)
				.leftJoin(member.team, team).on(team.name.eq("위"))
				.fetch();
		
		for(Tuple tuple : result) {
			System.out.println(tuple.toString());
		}
		/**
		 * [Member(id=3, username=여몽, age=40), null]
		 * [Member(id=4, username=육손, age=20), null]
		 * [Member(id=5, username=장합, age=42), Team(id=2, name=위)]
		 * [Member(id=6, username=학소, age=22), Team(id=2, name=위)]
		 */
	}
	
	/**
	 * 연관관계 없는 엔티티 외부 조인
	 * 회원의 이름이 팀 이름과 같은 대상 외부 조인
	 */
	@Test
	public void joinOnNoRelation() {
		em.persist(new Member("위"));
		em.persist(new Member("촉"));
		em.persist(new Member("오"));
		/** 
		select
	        member1,
	        team 
	    from
	        Member member1   
	    left join
	        Team team with member1.username = team.name 
	    */
		/*
		select
            member0_.member_id as member_i1_0_0_,
            team1_.team_id as team_id1_1_1_,
            member0_.age as age2_0_0_,
            member0_.team_id as team_id4_0_0_,
            member0_.username as username3_0_0_,
            team1_.name as name2_1_1_ 
        from
            member member0_ 
        left outer join
            team team1_ 
                on (
                    member0_.username=team1_.name
		*/
		List<Tuple> result = queryFactory
						.select(member, team)
						.from(member)
						.leftJoin(team).on(member.username.eq(team.name))
						.fetch();
		
		for(Tuple tuple : result) {
			System.out.println(tuple.toString());
		}
		/**
		 * [Member(id=3, username=여몽, age=40), null]
		 * [Member(id=4, username=육손, age=20), null]
		 * [Member(id=5, username=장합, age=42), null]
		 * [Member(id=6, username=학소, age=22), null]
		 * [Member(id=7, username=위, age=0), Team(id=2, name=위)]
		 * [Member(id=8, username=촉, age=0), null]
		 * [Member(id=9, username=오, age=0), Team(id=1, name=오)]
		 */
	}
	
	@PersistenceUnit
	EntityManagerFactory emf;
	
	@Test
	public void fetchJoinNo() {
		em.flush();
		em.clear();
		
		Member findMember = queryFactory
				.selectFrom(member)
				.where(member.username.eq("학소"))
				.fetchOne();
		
		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("페치 조인 미적용").isFalse();
	}
	
	@Test
	public void fetchJoinYes() {
		em.flush();
		em.clear();
		
		Member findMember = queryFactory
				.selectFrom(member)
				.join(member.team, team).fetchJoin()
				.where(member.username.eq("학소"))
				.fetchOne();
		
		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("페치 조인 적용").isTrue();
	}
	
	/**
	 * 나이가 가장 많은 장수
	 */
	@Test
	public void subQuery() {
		QMember memberSub = new QMember("memberSub");
		
		List<Member> result = queryFactory
					.selectFrom(member)
					.where(member.age.eq(
								JPAExpressions
									.select(memberSub.age.max())
									.from(memberSub)
					))
					.fetch();
		
		assertThat(result).extracting("age").containsExactly(42);
	}
	
	/**
	 * 나이가 평균 이상인 장수
	 */
	@Test
	public void subQueryGoe() {
		QMember memberSub = new QMember("memberSub");
		
		List<Member> result = queryFactory
					.selectFrom(member)
					.where(member.age.goe(
								JPAExpressions
									.select(memberSub.age.avg())
									.from(memberSub)
					))
					.fetch();
		
		assertThat(result).extracting("username").containsExactly("여몽", "장합");
	}
	
	/**
	 * 오나라 장수 in절로 구하기
	 */
	@Test
	public void subQueryIn() {
		QMember memberSub = new QMember("memberSub");
		
		List<Member> result = queryFactory
					.selectFrom(member)
					.where(member.username.in(
								JPAExpressions
									.select(memberSub.username)
									.from(memberSub)
									.join(memberSub.team, team)
									.where(team.name.eq("오"))
					))
					.fetch();
		
		assertThat(result).extracting("username").containsExactly("여몽", "육손");
	}
	
	/**
	 * 스칼라 서브쿼리
	 * 참고 : JPQL과 QueryDSL 모두, 인라인뷰 서브쿼리는 지원하지 않는다.
	 */
	@Test
	public void scalaSubQuery() {
		QMember memberSub = new QMember("memberSub");
		
		List<Tuple> result = queryFactory
					.select(member.username,
							JPAExpressions
								.select(memberSub.age.avg())
								.from(memberSub)
							)
					.from(member)
					.fetch();
		
		for (Tuple tuple : result) {
			System.out.println(tuple);
		}
		/**
		 * [여몽, 31.0]
		 * [육손, 31.0]
		 * [장합, 31.0]
		 * [학소, 31.0]
		 */
	}
	
	@Test
	public void basicCase() {
		List<String> result = queryFactory
					.select(member.age
							.when(20).then("스무살")
							.when(40).then("마흔살")
							.otherwise("기타"))
					.from(member)
					.fetch();
		
		result.stream().forEach(System.out::println);
		/**
		 * 마흔살
		 * 스무살
		 * 기타
		 * 기타
		 */
	}
	
	@Test
	public void complexCase() {
		List<String> result = queryFactory
				.select(new CaseBuilder()
						.when(member.age.between(0, 30)).then("0~30세")
						.when(member.age.between(31, 40)).then("31~40세")
						.otherwise("기타"))
				.from(member)
				.fetch();
		
		result.stream().forEach(System.out::println);
		/**
		 * 31~40세
		 * 0~30세
		 * 기타
		 * 0~30세
		 */
	}
	
	@Test
	public void constant() {
		/* 
		JPQL에 상수가 함께 작성된 형태는 아니다.(SQL도 마찬가지)
		select
        	member1.username 
    	from
        	Member member1 */
		List<Tuple> result = queryFactory
				.select(member.username, Expressions.constant("철기병"))
				.from(member)
				.fetch();
		
		for (Tuple tuple : result) {
			System.out.println(tuple);
		}
		/**
		 * [여몽, 철기병]
		 * [육손, 철기병]
		 * [장합, 철기병]
		 * [학소, 철기병]
		 */
	}
	
	@Test
	public void concat() {
		//{username}_{'거병나이'age}
		/**
		select
            ((member0_.username||?)||cast(member0_.age as char)) as col_0_0_ 
        from
            member member0_ 
        where
            member0_.username=?
		 */
		List<String> result = queryFactory
				.select(member.username.concat("_거병나이").concat(member.age.stringValue()))
				.from(member)
				.where(member.username.eq("육손"))
				.fetch();
		
		result.stream().forEach(System.out::println);
		//육손_거병나이20
	}
}
