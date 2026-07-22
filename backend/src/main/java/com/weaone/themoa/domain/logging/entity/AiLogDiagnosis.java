package com.weaone.themoa.domain.logging.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * 에러 한 건당 현재 AI 진단 최대 1행(managelogging.md §4-2, erd.md §9). 재분석 이력은 별도 행으로 보관하지
 * 않고 같은 행을 재사용한다. {@link #errorLog} 삭제 시 함께 삭제된다({@code ON DELETE CASCADE}).
 */
@Entity
@Table(name = "ai_log_diagnosis",
        indexes = {
                @Index(name = "idx_ai_log_diagnosis_status", columnList = "status, updated_at, id"),
                @Index(name = "idx_ai_log_diagnosis_created", columnList = "created_at, id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiLogDiagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "error_log_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ErrorLog errorLog;

    /** 분석을 요청한 ADMIN. 탈퇴 시 NULL(erd.md §9). */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "requested_by_member_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiDiagnosisStatus status;

    @Column(name = "cause_category", length = 50)
    private String causeCategory;

    @Column(length = 255)
    private String summary;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "failure_message", length = 1000)
    private String failureMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private AiLogDiagnosis(ErrorLog errorLog, Member requestedBy, String modelName, LocalDateTime now) {
        this.errorLog = errorLog;
        this.requestedBy = requestedBy;
        this.status = AiDiagnosisStatus.PENDING;
        this.modelName = modelName;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static AiLogDiagnosis createPending(ErrorLog errorLog, Member requestedBy, String modelName,
                                                LocalDateTime now) {
        return new AiLogDiagnosis(errorLog, requestedBy, modelName, now);
    }

    /** COMPLETED/FAILED 상태에서 재요청 시 같은 행을 PENDING으로 초기화한다(§5-4). */
    public void resetToPending(Member requestedBy, LocalDateTime now) {
        this.requestedBy = requestedBy;
        this.status = AiDiagnosisStatus.PENDING;
        this.causeCategory = null;
        this.summary = null;
        this.rootCause = null;
        this.recommendedAction = null;
        this.failureMessage = null;
        this.completedAt = null;
        this.updatedAt = now;
    }

    public void complete(String causeCategory, String summary, String rootCause, String recommendedAction,
                          LocalDateTime now) {
        this.status = AiDiagnosisStatus.COMPLETED;
        this.causeCategory = causeCategory;
        this.summary = summary;
        this.rootCause = rootCause;
        this.recommendedAction = recommendedAction;
        this.failureMessage = null;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void fail(String failureMessage, LocalDateTime now) {
        this.status = AiDiagnosisStatus.FAILED;
        this.causeCategory = null;
        this.summary = null;
        this.rootCause = null;
        this.recommendedAction = null;
        this.failureMessage = failureMessage;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public Long getRequestedByMemberId() {
        return requestedBy == null ? null : requestedBy.getId();
    }
}
