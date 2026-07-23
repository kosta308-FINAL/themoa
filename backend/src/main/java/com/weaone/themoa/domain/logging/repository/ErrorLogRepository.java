package com.weaone.themoa.domain.logging.repository;

import com.weaone.themoa.domain.logging.dto.AdminErrorLogListItemResponse;
import com.weaone.themoa.domain.logging.entity.ErrorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    /** 관리자 목록(§5-2): 필터는 모두 선택이며 정확히 일치, 기본 정렬 created_at DESC, id DESC. */
    @Query("""
            select new com.weaone.themoa.domain.logging.dto.AdminErrorLogListItemResponse(
                e.id, e.traceId, e.member.id, e.httpMethod, e.requestUri, e.controller,
                e.statusCode, e.exceptionClass, e.errorMessage, d.status, e.createdAt)
            from ErrorLog e
            left join AiLogDiagnosis d on d.errorLog = e
            where (:exceptionClass is null or e.exceptionClass = :exceptionClass)
              and (:requestUri is null or e.requestUri = :requestUri)
              and (:controller is null or e.controller = :controller)
              and (:memberId is null or e.member.id = :memberId)
              and (:startAt is null or e.createdAt >= :startAt)
              and (:endAt is null or e.createdAt < :endAt)
            order by e.createdAt desc, e.id desc
            """)
    Page<AdminErrorLogListItemResponse> searchForAdmin(@Param("exceptionClass") String exceptionClass,
                                                         @Param("requestUri") String requestUri,
                                                         @Param("controller") String controller,
                                                         @Param("memberId") Long memberId,
                                                         @Param("startAt") LocalDateTime startAt,
                                                         @Param("endAt") LocalDateTime endAt,
                                                         Pageable pageable);

    /** 보관 배치(§7-2): 오래된 순으로 최대 {@code pageable.getPageSize()}개의 ID만 뽑아 청크 삭제한다. */
    @Query("select e.id from ErrorLog e where e.createdAt < :threshold order by e.createdAt asc, e.id asc")
    List<Long> findIdsCreatedBefore(@Param("threshold") LocalDateTime threshold, Pageable pageable);

    long countByCreatedAtBefore(LocalDateTime threshold);
}
