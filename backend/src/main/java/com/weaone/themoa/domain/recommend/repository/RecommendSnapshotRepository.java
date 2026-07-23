package com.weaone.themoa.domain.recommend.repository;

import com.weaone.themoa.domain.recommend.entity.RecommendSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendSnapshotRepository extends JpaRepository<RecommendSnapshot, Long> {

    List<RecommendSnapshot> findByMember_IdOrderByRankNoAsc(Long memberId);

    /** 추천을 다시 받으면 이전 기록을 지우고 새 결과로 교체한다(최신 1회분만 유지). */
    void deleteByMember_Id(Long memberId);
}
