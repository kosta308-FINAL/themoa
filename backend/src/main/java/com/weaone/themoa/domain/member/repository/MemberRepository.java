package com.weaone.themoa.domain.member.repository;

import com.weaone.themoa.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    /** 매 요청 토큰 검증용. 회원 엔티티 전체를 로딩하지 않도록 필요한 값만 읽는다. */
    @Query("select m.tokenVersion from Member m where m.id = :memberId")
    Optional<Integer> findTokenVersionById(@Param("memberId") Long memberId);
}