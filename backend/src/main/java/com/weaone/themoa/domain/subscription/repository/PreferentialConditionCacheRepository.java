package com.weaone.themoa.domain.subscription.repository;

import com.weaone.themoa.domain.subscription.entity.PreferentialConditionCache;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PreferentialConditionCacheRepository extends JpaRepository<PreferentialConditionCache, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<PreferentialConditionCache> findWithItemsByProductId(Long productId);

    /** 관리자 화면: 원문이 바뀌어 재검토가 필요한(잠긴+stale) 캐시 목록. */
    @EntityGraph(attributePaths = "items")
    List<PreferentialConditionCache> findByStaleTrue();

    /** 상품 목록 화면: 여러 상품의 캐시 상태를 한 번에 조회. */
    @EntityGraph(attributePaths = "items")
    List<PreferentialConditionCache> findByProductIdIn(Collection<Long> productIds);
}
