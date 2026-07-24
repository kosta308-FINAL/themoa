package com.weaone.themoa.domain.notification.repository;

import com.weaone.themoa.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByMember_IdAndDedupKey(Long memberId, String dedupKey);

    /** 앱 내 알림 목록(알림.md MOA-S-NOT-APP-02). 최신순. */
    Page<Notification> findByMember_IdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Optional<Notification> findByIdAndMember_Id(Long id, Long memberId);

    /** 읽지 않은 알림 배지 기준(알림.md §A "읽음 상태 컬럼 신규"). */
    long countByMember_IdAndReadFalse(Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification n
               set n.read = true,
                   n.readAt = :readAt
             where n.member.id = :memberId
               and n.read = false
            """)
    int markAllReadByMemberId(
            @Param("memberId") Long memberId,
            @Param("readAt") LocalDateTime readAt
    );
}
