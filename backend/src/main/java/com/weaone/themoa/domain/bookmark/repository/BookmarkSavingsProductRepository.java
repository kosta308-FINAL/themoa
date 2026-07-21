package com.weaone.themoa.domain.bookmark.repository;

import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 북마크 목록에 표시할 예·적금 상세를 읽는 전용 레포지토리.
 * 상품 엔티티는 recommend 도메인 것을 재사용하고, 대표금리 계산에 필요한 options를 fetch join으로 같이 가져와
 * 북마크 개수만큼 추가 쿼리가 나가는 N+1을 막는다.
 */
public interface BookmarkSavingsProductRepository extends JpaRepository<SavingsProduct, Long> {

    @Query("""
            select distinct p from SavingsProduct p
            left join fetch p.options
            where p.id in :ids
            """)
    List<SavingsProduct> findAllWithOptionsByIdIn(@Param("ids") Collection<Long> ids);
}
