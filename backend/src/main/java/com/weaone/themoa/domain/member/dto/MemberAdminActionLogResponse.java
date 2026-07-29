package com.weaone.themoa.domain.member.dto;

import com.weaone.themoa.domain.member.entity.MemberAdminActionLog;
import com.weaone.themoa.domain.member.entity.MemberAdminActionType;

import java.time.LocalDateTime;

/** 회원 상세 화면의 "관리자 조치 이력" 한 행. adminId는 행위자가 탈퇴했으면 null일 수 있다. */
public record MemberAdminActionLogResponse(
        Long id,
        Long adminId,
        MemberAdminActionType actionType,
        String reason,
        LocalDateTime createdAt
) {

    public static MemberAdminActionLogResponse from(MemberAdminActionLog log) {
        return new MemberAdminActionLogResponse(
                log.getId(),
                log.getAdminId(),
                log.getActionType(),
                log.getReason(),
                log.getCreatedAt()
        );
    }
}
