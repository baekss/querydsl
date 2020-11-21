package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;
import study.querydsl.repository.MemberRepository;

@SpringBootTest
@Transactional
public class MemberRepositoryTest {

	@Autowired
	EntityManager em;
	
	@Autowired
	MemberRepository memberRepository;
	
	@BeforeEach
	public void before() {
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
	public void basicTest() {
		Member member = new Member("하후돈", 30);
		memberRepository.save(member);
		
		Member findMember = memberRepository
						.findById(member.getId())
						.orElse(new Member("재야장수"));
		
		assertThat(findMember).isEqualTo(member);
		
		List<Member> result1 = memberRepository.findAll();
		assertThat(result1).containsExactly(member);
		
		List<Member> result2 = memberRepository.findByUsername("하후돈");
		assertThat(result2).containsExactly(member);
	}
	
	@Test
	public void searchTest() {
		MemberSearchCondition condition = new MemberSearchCondition();
		condition.setAgeGoe(21);
		condition.setAgeLoe(25);
		condition.setTeamName("위");
		
		List<MemberTeamDto> result = memberRepository.search(condition);
		assertThat(result).extracting("username").containsExactly("학소");
	}
	
	@Test
	public void searchPagingTest() {
		MemberSearchCondition condition = new MemberSearchCondition();
		PageRequest pageable = PageRequest.of(0, 3);
		Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageable);
		assertThat(result.getSize()).isEqualTo(3);
		assertThat(result.getContent()).extracting("username").containsExactly("여몽", "육손", "장합");
	}
}
