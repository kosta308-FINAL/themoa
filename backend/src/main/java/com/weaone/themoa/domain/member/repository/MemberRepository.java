package com.weaone.themoa.domain.member.repository;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    /** 아이디(이메일) 찾기: 닉네임은 unique가 아니라 여러 건이 나올 수 있다(AuthService에서 0/1/N을 구분한다). */
    List<Member> findByNameAndBirthDateAndWithdrawnAtIsNull(String name, LocalDate birthDate);

    /** 매 요청 토큰 검증용. 회원 엔티티 전체를 로딩하지 않도록 필요한 값만 읽는다. */
    @Query("select m.tokenVersion from Member m where m.id = :memberId")
    Optional<Integer> findTokenVersionById(@Param("memberId") Long memberId);

    /** 세션 경계를 넘어 전달된 detached 엔티티의 지연 로딩 member를 다시 타지 않기 위한 최소 조회. */
    @Query("select m.payday from Member m where m.id = :memberId")
    Optional<Integer> findPaydayById(@Param("memberId") Long memberId);

    /** 습관 코칭 월급일 배치 대상(habitExpense.md §3) 후보 — 급여 주기가 있는 회원만 골라낸다. */
    List<Member> findByPaydayIsNotNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :memberId")
    Optional<Member> findByIdForUpdate(@Param("memberId") Long memberId);

    /** 관리자 회원 목록(personalinfo.md — 최소 이용): 이메일·회원ID는 정확히 일치만 지원한다. */
    @Query("""
            select m from Member m
            where (:email is null or m.email = :email)
              and (:memberId is null or m.id = :memberId)
              and (:includeWithdrawn = true or m.withdrawnAt is null)
              and (:lockedOnly = false or (m.lockedUntil is not null and m.lockedUntil > :now))
            order by m.createdAt desc, m.id desc
            """)
    Page<Member> searchForAdmin(@Param("email") String email,
                                 @Param("memberId") Long memberId,
                                 @Param("includeWithdrawn") boolean includeWithdrawn,
                                 @Param("lockedOnly") boolean lockedOnly,
                                 @Param("now") LocalDateTime now,
                                 Pageable pageable);
}
