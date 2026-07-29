package com.weaone.themoa.domain.member.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.member.dto.AdminMemberDetailResponse;
import com.weaone.themoa.domain.member.dto.AdminMemberForceLogoutRequest;
import com.weaone.themoa.domain.member.dto.AdminMemberListResponse;
import com.weaone.themoa.domain.member.dto.AdminMemberLockRequest;
import com.weaone.themoa.domain.member.dto.AdminMemberUnlockRequest;
import com.weaone.themoa.domain.member.dto.AdminMemberWithdrawRequest;
import com.weaone.themoa.domain.member.dto.MemberAdminActionLogListResponse;
import com.weaone.themoa.domain.member.service.AdminMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 회원 조회·계정 조치(personalinfo.md). {@code SecurityConfig}가 ROLE_ADMIN만 통과시킨다.
 * 관리자 권한(Role) 부여/해제는 이 API로 노출하지 않는다(기존 설계대로 운영자가 DB에서 직접 처리).
 */
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @Operation(summary = "회원 목록(관리자)",
            description = "email·memberId는 정확히 일치만 지원합니다. includeWithdrawn=false면 탈퇴 회원은 제외합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminMemberListResponse>> list(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false, defaultValue = "false") boolean includeWithdrawn,
            @RequestParam(required = false, defaultValue = "false") boolean lockedOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminMemberService.list(email, memberId, includeWithdrawn, lockedOnly, page, size)));
    }

    @Operation(summary = "회원 상세(관리자)",
            description = "마스킹되지 않은 이메일·생년월일을 반환하며, 이 조회 자체가 감사 로그(VIEW_DETAIL)에 기록됩니다.")
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<AdminMemberDetailResponse>> detail(
            @Parameter(hidden = true) @AuthenticationPrincipal Long adminId,
            @PathVariable Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(adminMemberService.detail(memberId, adminId)));
    }

    @Operation(summary = "계정 잠금(관리자)", description = "해제 전까지 유지됩니다. 사유는 필수입니다.")
    @PostMapping("/{memberId}/lock")
    public ResponseEntity<ApiResponse<Void>> lock(
            @Parameter(hidden = true) @AuthenticationPrincipal Long adminId,
            @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberLockRequest request) {
        adminMemberService.lock(memberId, adminId, request.reason());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "계정 잠금 해제(관리자)")
    @PostMapping("/{memberId}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlock(
            @Parameter(hidden = true) @AuthenticationPrincipal Long adminId,
            @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberUnlockRequest request) {
        adminMemberService.unlock(memberId, adminId, request.reason());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "강제 로그아웃(관리자)", description = "이 회원의 모든 기기 세션을 즉시 무효화합니다.")
    @PostMapping("/{memberId}/force-logout")
    public ResponseEntity<ApiResponse<Void>> forceLogout(
            @Parameter(hidden = true) @AuthenticationPrincipal Long adminId,
            @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberForceLogoutRequest request) {
        adminMemberService.forceLogout(memberId, adminId, request.reason());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "강제 탈퇴(관리자)", description = "되돌릴 수 없습니다. 이메일·닉네임이 즉시 익명화됩니다. 사유는 필수입니다.")
    @PostMapping("/{memberId}/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal Long adminId,
            @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberWithdrawRequest request) {
        adminMemberService.forceWithdraw(memberId, adminId, request.reason());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "회원별 관리자 조치 이력", description = "잠금/해제/강제로그아웃/강제탈퇴/상세열람 이력을 최신순으로 반환합니다.")
    @GetMapping("/{memberId}/action-logs")
    public ResponseEntity<ApiResponse<MemberAdminActionLogListResponse>> actionLogs(
            @PathVariable Long memberId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ApiResponse.success(adminMemberService.actionLogs(memberId, page, size)));
    }
}
