package com.weaone.themoa.domain.member.dto;

import com.weaone.themoa.domain.member.entity.EntryMode;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.entity.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code GET /api/admin/members/{memberId}} 응답. 이 화면은 마스킹을 해제해 실제 이메일·생년월일을 보여주므로
 * 조회 자체가 {@code MemberAdminActionLog}에 VIEW_DETAIL로 기록된다. 소득유형·월급·시급·저축목표 등 금융
 * 프로필 필드는 계정 관리 목적과 무관해 의도적으로 포함하지 않는다(최소 수집·이용 원칙).
 */
public record AdminMemberDetailResponse(
        Long id,
        String email,
        String name,
        Gender gender,
        LocalDate birthDate,
        Role role,
        EntryMode entryMode,
        boolean cardSyncEnabled,
        AdminMemberStatus status,
        int loginFailCount,
        LocalDateTime lockedUntil,
        LocalDateTime lastActiveAt,
        LocalDateTime createdAt,
        LocalDateTime withdrawnAt,
        List<String> linkedProviders
) {

    public static AdminMemberDetailResponse of(Member member, List<String> linkedProviders, LocalDateTime now) {
        return new AdminMemberDetailResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getGender(),
                member.getBirthDate(),
                member.getRole(),
                member.getEntryMode(),
                member.isCardSyncEnabled(),
                AdminMemberStatus.of(member.getLockedUntil(), member.getWithdrawnAt(), now),
                member.getLoginFailCount(),
                member.getLockedUntil(),
                member.getLastActiveAt(),
                member.getCreatedAt(),
                member.getWithdrawnAt(),
                linkedProviders
        );
    }
}
