package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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
		queryFactory = new JPAQueryFactory(em);
		
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
}
