package com.weaone.themoa.domain.recommend.repository;

import com.weaone.themoa.domain.budget.entity.SurplusFund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 추천 폼 기본값(월 납입가능금액)에 쓸 잉여금을 읽는 전용 레포지토리.
 * 엔티티는 budget 도메인 것을 재사용하되, 조회 메서드는 여기서 따로 정의한다
 * (팀원이 관리하는 {@code SurplusFundRepository}를 수정하지 않기 위함).
 */
public interface RecommendSurplusFundRepository extends JpaRepository<SurplusFund, Long> {

    /** 가장 최근 급여주기의 잉여금. year_month는 "YYYY-MM" 문자열이라 사전순 정렬이 곧 시간순이다. */
    Optional<SurplusFund> findFirstByMember_IdOrderByYearMonthDesc(Long memberId);
}
