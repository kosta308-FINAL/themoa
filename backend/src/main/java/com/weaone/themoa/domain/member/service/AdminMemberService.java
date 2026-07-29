package com.weaone.themoa.domain.member.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.auth.repository.MemberSocialAccountRepository;
import com.weaone.themoa.domain.auth.service.AuthTokenService;
import com.weaone.themoa.domain.member.dto.AdminMemberDetailResponse;
import com.weaone.themoa.domain.member.dto.AdminMemberListItemResponse;
import com.weaone.themoa.domain.member.dto.AdminMemberListResponse;
import com.weaone.themoa.domain.member.dto.MemberAdminActionLogListResponse;
import com.weaone.themoa.domain.member.dto.MemberAdminActionLogResponse;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.entity.MemberAdminActionLog;
import com.weaone.themoa.domain.member.entity.MemberAdminActionType;
import com.weaone.themoa.domain.member.repository.MemberAdminActionLogRepository;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 회원 조회·계정 조치(personalinfo.md). 관리자 권한(Role) 변경은 이 서비스의 범위가 아니다 —
 * {@link Member#changeRole(com.weaone.themoa.domain.member.entity.Role)}의 Javadoc대로 운영자가 DB에서
 * 직접 처리하는 영역으로 남겨둔다.
 */
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int ACTION_LOG_DEFAULT_SIZE = 20;

    private final MemberRepository memberRepository;
    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final MemberAdminActionLogRepository memberAdminActionLogRepository;
    private final AuthTokenService authTokenService;

    @Transactional(readOnly = true)
    public AdminMemberListResponse list(String email, Long memberId, boolean includeWithdrawn, boolean lockedOnly,
                                         Integer page, Integer size) {
        LocalDateTime now = LocalDateTime.now();
        Page<Member> members = memberRepository.searchForAdmin(
                normalizeEmail(email), memberId, includeWithdrawn, lockedOnly, now,
                PageRequest.of(normalizePage(page), clampSize(size)));
        return AdminMemberListResponse.from(members.map(member -> AdminMemberListItemResponse.from(member, now)));
    }

    /** 상세 열람 자체가 마스킹 해제된 개인정보 접근이므로, 이 트랜잭션 안에서 VIEW_DETAIL 감사 로그까지 남긴다. */
    @Transactional
    public AdminMemberDetailResponse detail(Long memberId, Long adminId) {
        LocalDateTime now = LocalDateTime.now();
        Member member = getMemberOrThrow(memberId);
        List<String> linkedProviders = memberSocialAccountRepository.findByMember_Id(memberId).stream()
                .map(account -> account.getProvider().name())
                .toList();
        recordAction(adminId, member, MemberAdminActionType.VIEW_DETAIL, null, now);
        return AdminMemberDetailResponse.of(member, linkedProviders, now);
    }

    @Transactional
    public void lock(Long memberId, Long adminId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        Member member = getMemberOrThrow(memberId);
        guardSelfAction(memberId, adminId);
        member.adminLock(now.plusYears(100));
        recordAction(adminId, member, MemberAdminActionType.LOCK, reason, now);
    }

    @Transactional
    public void unlock(Long memberId, Long adminId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        Member member = getMemberOrThrow(memberId);
        member.adminUnlock();
        recordAction(adminId, member, MemberAdminActionType.UNLOCK, reason, now);
    }

    @Transactional
    public void forceLogout(Long memberId, Long adminId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        Member member = getMemberOrThrow(memberId);
        guardSelfAction(memberId, adminId);
        authTokenService.revokeAll(member);
        recordAction(adminId, member, MemberAdminActionType.FORCE_LOGOUT, reason, now);
    }

    /** AuthService.withdraw(마이페이지 탈퇴)와 동일한 순서로 처리한다: 소셜 연결 제거 → 익명화 → 전 세션 폐기. */
    @Transactional
    public void forceWithdraw(Long memberId, Long adminId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        Member member = getMemberOrThrow(memberId);
        guardSelfAction(memberId, adminId);
        memberSocialAccountRepository.deleteByMember_Id(memberId);
        member.withdraw(now);
        authTokenService.revokeAll(member);
        recordAction(adminId, member, MemberAdminActionType.FORCE_WITHDRAW, reason, now);
    }

    @Transactional(readOnly = true)
    public MemberAdminActionLogListResponse actionLogs(Long memberId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(normalizePage(page), clampActionLogSize(size));
        Page<MemberAdminActionLogResponse> result = memberAdminActionLogRepository
                .findByTargetMember_IdOrderByCreatedAtDesc(memberId, pageable)
                .map(MemberAdminActionLogResponse::from);
        return MemberAdminActionLogListResponse.from(result);
    }

    private Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_MEMBER_NOT_FOUND));
    }

    private void guardSelfAction(Long memberId, Long adminId) {
        if (memberId.equals(adminId)) {
            throw new BusinessException(ErrorCode.ADMIN_MEMBER_SELF_ACTION_FORBIDDEN);
        }
    }

    private void recordAction(Long adminId, Member target, MemberAdminActionType actionType, String reason,
                               LocalDateTime now) {
        Member admin = memberRepository.getReferenceById(adminId);
        memberAdminActionLogRepository.save(MemberAdminActionLog.create(admin, target, actionType, reason, now));
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int clampSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private int clampActionLogSize(Integer size) {
        if (size == null || size <= 0) {
            return ACTION_LOG_DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
