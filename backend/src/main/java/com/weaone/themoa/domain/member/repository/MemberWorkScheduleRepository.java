package com.weaone.themoa.domain.member.repository;

import com.weaone.themoa.domain.member.entity.MemberWorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberWorkScheduleRepository extends JpaRepository<MemberWorkSchedule, Long> {

    List<MemberWorkSchedule> findByMember_Id(Long memberId);

    /** 설정 변경 시 전체 교체(개별 UPDATE 대신 삭제 후 재생성)에 쓴다. */
    @Modifying
    @Query("delete from MemberWorkSchedule s where s.member.id = :memberId")
    void deleteByMember_Id(@Param("memberId") Long memberId);
}
