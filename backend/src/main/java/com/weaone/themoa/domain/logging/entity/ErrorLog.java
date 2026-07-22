package com.weaone.themoa.domain.logging.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 예상하지 못한 500 한 건(managelogging.md §3, erd.md §9). {@code GlobalExceptionHandler.handleUnexpected}가
 * 잡은 예외와, {@code handleBusiness}가 잡았지만 500 상태코드로 정의된 {@code BusinessException}만 여기 쌓인다.
 * 회원 탈퇴 후에도 장애 기록은 보존하되 {@link #member}만 NULL로 비운다.
 */
@Entity
@Table(name = "error_log",
        indexes = {
                @Index(name = "idx_error_log_created", columnList = "created_at, id"),
                @Index(name = "idx_error_log_exception_class", columnList = "exception_class, created_at, id"),
                @Index(name = "idx_error_log_request_uri", columnList = "request_uri, created_at, id"),
                @Index(name = "idx_error_log_controller", columnList = "controller, created_at, id"),
                @Index(name = "idx_error_log_member", columnList = "member_id, created_at, id"),
                @Index(name = "idx_error_log_trace", columnList = "trace_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false, length = 36)
    private String traceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "member_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member member;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "request_uri", nullable = false, length = 255)
    private String requestUri;

    /** {@code {컨트롤러 클래스명}.{메서드명}}. 라우팅되지 않은 경우 "unmatched"(managelogging.md §2-1). */
    @Column(name = "controller", nullable = false, length = 150)
    private String controller;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    /** 예상 못한 예외는 FQCN, 500 BusinessException은 {@code BusinessException:{ErrorCode 이름}}(§0-3). */
    @Column(name = "exception_class", nullable = false, length = 255)
    private String exceptionClass;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "stack_trace_excerpt", nullable = false, columnDefinition = "TEXT")
    private String stackTraceExcerpt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private ErrorLog(String traceId, Member member, String httpMethod, String requestUri, String controller,
                      int statusCode, String exceptionClass, String errorMessage, String stackTraceExcerpt,
                      LocalDateTime createdAt) {
        this.traceId = traceId;
        this.member = member;
        this.httpMethod = httpMethod;
        this.requestUri = requestUri;
        this.controller = controller;
        this.statusCode = statusCode;
        this.exceptionClass = exceptionClass;
        this.errorMessage = errorMessage;
        this.stackTraceExcerpt = stackTraceExcerpt;
        this.createdAt = createdAt;
    }

    public static ErrorLog create(String traceId, Member member, String httpMethod, String requestUri,
                                   String controller, int statusCode, String exceptionClass, String errorMessage,
                                   String stackTraceExcerpt, LocalDateTime createdAt) {
        return new ErrorLog(traceId, member, httpMethod, requestUri, controller, statusCode, exceptionClass,
                errorMessage, stackTraceExcerpt, createdAt);
    }

    public Long getMemberId() {
        return member == null ? null : member.getId();
    }
}
