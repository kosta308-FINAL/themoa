package com.weaone.themoa.domain.auth.repository;

import com.weaone.themoa.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("select rt from RefreshToken rt join fetch rt.member where rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findWithMemberByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * 삭제된 행 수를 돌려준다. 같은 토큰으로 동시에 재발급이 들어와도 DELETE가 원자적이라
     * 정확히 한 요청만 1을 받는다. 선조회 결과를 믿고 rotation하면 둘 다 성공해 버린다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken rt where rt.tokenHash = :tokenHash")
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken rt where rt.member.id = :memberId")
    int deleteAllByMemberId(@Param("memberId") Long memberId);
}