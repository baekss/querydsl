package study.querydsl.repository;

import java.util.List;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

public interface MemberRepositoryQuerydsl {
	List<MemberTeamDto> search(MemberSearchCondition condition);
}
