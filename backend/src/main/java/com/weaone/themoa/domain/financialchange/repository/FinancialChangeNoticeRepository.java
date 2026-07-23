package com.weaone.themoa.domain.financialchange.repository;

import com.weaone.themoa.domain.financialchange.entity.FinancialChangeNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancialChangeNoticeRepository extends JpaRepository<FinancialChangeNotice, Long> {

    List<FinancialChangeNotice> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /** 알림에서 넘어왔을 때 해당 변경 내역을 찾는다(알림의 dedupKey로 역추적). */
    Optional<FinancialChangeNotice> findByMemberIdAndDedupKey(Long memberId, String dedupKey);

    /** 같은 상품의 변동 추이(최근 것부터). 팝업에서 "이전 변동 이력"을 보여줄 때 쓴다. */
    List<FinancialChangeNotice> findTop10ByMemberIdAndTargetTypeAndTargetIdOrderByCreatedAtDesc(
            Long memberId,
            com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType targetType,
            Long targetId);
}
