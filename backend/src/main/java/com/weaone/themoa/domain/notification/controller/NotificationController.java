package com.weaone.themoa.domain.notification.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.notification.dto.response.NotificationListResponse;
import com.weaone.themoa.domain.notification.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 앱 내 알림 목록·읽음 처리(알림.md MOA-S-NOT-APP-02·-04·-05). 웹 SPA라 알림 전달 채널은 이 목록뿐이다. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @Operation(summary = "앱 내 알림 목록 조회",
            description = "로그인 사용자의 알림을 최신순으로 페이지 조회합니다. unreadCount는 읽지 않은 알림 배지 수입니다. 먼저 /api/auth/login으로 로그인하세요.")
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(notificationQueryService.list(memberId, pageable)));
    }

    @Operation(summary = "알림 읽음 처리",
            description = "선택한 알림을 읽음으로 표시합니다. 먼저 /api/notifications에서 읽음 처리할 notificationId를 확인하세요.")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Parameter(description = "읽음 처리할 알림 ID") @PathVariable Long notificationId) {
        notificationQueryService.markRead(memberId, notificationId);
        return ResponseEntity.noContent().build();
    }
}
