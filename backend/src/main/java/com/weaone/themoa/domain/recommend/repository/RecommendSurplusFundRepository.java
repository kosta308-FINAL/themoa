package com.weaone.themoa.domain.recommend.repository;

import com.weaone.themoa.domain.budget.entity.SurplusFund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 추천 폼 기본값(월 납입가능금액)에 쓸 잉여금을 읽는 전용 레포지토리.
 * 엔티티는 budget 도메인 것을 재사용하되, 조회 메서드는 여기서 따로 정의한다
 * (팀원이 관리하는 {@code SurplusFundRepository}를 수정하지 않기 위함).
 */
public interface RecommendSurplusFundRepository extends JpaRepository<SurplusFund, Long> {

    /** 회원의 전체 급여주기 잉여금(평균 계산용). 순서는 평균에 영향이 없어 정렬을 강제하지 않는다. */
    List<SurplusFund> findByMember_Id(Long memberId);
}
