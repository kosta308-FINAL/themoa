package com.weaone.themoa.domain.recommend.repository;

import com.weaone.themoa.domain.budget.entity.SurplusFundTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

/** 금융상품 추천 기본값 계산에서 회원이 이미 이번 주기들로 가져간 잉여금 합계를 읽는다. */
public interface RecommendSurplusFundTransferRepository extends JpaRepository<SurplusFundTransfer, Long> {

    @Query("""
            select coalesce(sum(t.amount), 0)
            from SurplusFundTransfer t
            where t.budget.member.id = :memberId
            """)
    BigDecimal sumAmountByMember_Id(@Param("memberId") Long memberId);
}
