package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.entity.Member;
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
}
