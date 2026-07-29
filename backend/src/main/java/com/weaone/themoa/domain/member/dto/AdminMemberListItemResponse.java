package com.weaone.themoa.domain.member.dto;

import com.weaone.themoa.domain.auth.support.EmailMasker;
import com.weaone.themoa.domain.member.entity.EntryMode;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.entity.Role;

import java.time.LocalDateTime;

/** 관리자 회원 목록 한 행. 이메일은 마스킹하고 금융 프로필(월급·저축목표 등)은 포함하지 않는다(최소 이용 원칙). */
public record AdminMemberListItemResponse(
        Long id,
        String maskedEmail,
        String name,
        Gender gender,
        EntryMode entryMode,
        Role role,
        AdminMemberStatus status,
        LocalDateTime createdAt,
        LocalDateTime lastActiveAt
) {

    public static AdminMemberListItemResponse from(Member member, LocalDateTime now) {
        return new AdminMemberListItemResponse(
                member.getId(),
                EmailMasker.mask(member.getEmail()),
                member.getName(),
                member.getGender(),
                member.getEntryMode(),
                member.getRole(),
                AdminMemberStatus.of(member.getLockedUntil(), member.getWithdrawnAt(), now),
                member.getCreatedAt(),
                member.getLastActiveAt()
        );
    }
}
