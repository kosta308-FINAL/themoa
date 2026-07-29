package com.weaone.themoa.domain.member.entity;

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
 * 회원 정보에 대한 관리자 조치 1건(개인정보보호법 접근기록 관리 의무 대응). 누가(admin) 언제 누구를(targetMember)
 * 무엇을(actionType) 왜(reason) 했는지 남긴다. 상세 열람(VIEW_DETAIL)도 남겨 마스킹 해제 조회를 추적한다.
 * 행위자·대상 회원이 이후 탈퇴해도 조치 기록 자체는 보존하고 FK만 NULL로 비운다.
 */
@Entity
@Table(name = "member_admin_action_log",
        indexes = {
                @Index(name = "idx_member_admin_action_log_target", columnList = "target_member_id, created_at"),
                @Index(name = "idx_member_admin_action_log_created", columnList = "created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberAdminActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "admin_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member admin;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "target_member_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member targetMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private MemberAdminActionType actionType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private MemberAdminActionLog(Member admin, Member targetMember, MemberAdminActionType actionType,
                                  String reason, LocalDateTime createdAt) {
        this.admin = admin;
        this.targetMember = targetMember;
        this.actionType = actionType;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public static MemberAdminActionLog create(Member admin, Member targetMember, MemberAdminActionType actionType,
                                               String reason, LocalDateTime createdAt) {
        return new MemberAdminActionLog(admin, targetMember, actionType, reason, createdAt);
    }

    public Long getAdminId() {
        return admin == null ? null : admin.getId();
    }

    public Long getTargetMemberId() {
        return targetMember == null ? null : targetMember.getId();
    }
}
