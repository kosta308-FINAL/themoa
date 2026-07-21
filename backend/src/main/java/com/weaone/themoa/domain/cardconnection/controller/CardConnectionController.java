package com.weaone.themoa.domain.cardconnection.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.cardconnection.dto.request.CardConnectionCreateRequest;
import com.weaone.themoa.domain.cardconnection.dto.request.CardSyncSettingRequest;
import com.weaone.themoa.domain.cardconnection.dto.response.CardConnectionListResponse;
import com.weaone.themoa.domain.cardconnection.dto.response.CardConnectionResponse;
import com.weaone.themoa.domain.cardconnection.dto.response.InitialSyncStatusResponse;
import com.weaone.themoa.domain.cardconnection.service.CardConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/card-connections")
@RequiredArgsConstructor
public class CardConnectionController {

    private final CardConnectionService cardConnectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<CardConnectionResponse>> connect(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CardConnectionCreateRequest request) {
        CardConnectionResponse response = cardConnectionService.connect(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** S-01 "카드 관리" 팝업(entryMode.md §2-1): 연결된 카드사 목록 + 자동수집 ON/OFF 상태. */
    @GetMapping
    public ResponseEntity<ApiResponse<CardConnectionListResponse>> list(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(cardConnectionService.list(memberId)));
    }

    @Operation(summary = "초기수집 상태 폴링", description = "완료·실패 시 폴링을 중단하세요(dayguide.md §2.4).")
    @GetMapping("/initial-sync-status")
    public ResponseEntity<ApiResponse<InitialSyncStatusResponse>> initialSyncStatus(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(cardConnectionService.getInitialSyncStatus(memberId)));
    }

    @Operation(summary = "초기수집 실패 재시도", description = "본인 커넥션이며 FAILED 상태일 때만 허용합니다. FETCHING부터 다시 시작합니다.")
    @PostMapping("/{connectionId}/initial-sync/retry")
    public ResponseEntity<Void> retryInitialSync(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long connectionId) {
        cardConnectionService.retryInitialSync(memberId, connectionId);
        return ResponseEntity.noContent().build();
    }

    /** 카드 자동수집 ON/OFF(entryMode.md §2-1). OFF→ON 전환은 서비스 계층이 갭 백필을 트리거한다(§4-1). */
    @PatchMapping("/sync-enabled")
    public ResponseEntity<Void> setCardSync(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CardSyncSettingRequest request) {
        cardConnectionService.setCardSyncEnabled(memberId, request.enabled(), request.recoveryMode());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "카드 연동 해제", description = "마이페이지: 연동을 해제하면 이후 자동수집·배치 대상에서 제외됩니다. 기존 수집 내역은 유지됩니다.")
    @DeleteMapping("/{connectionId}")
    public ResponseEntity<Void> disconnect(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long connectionId) {
        cardConnectionService.disconnect(memberId, connectionId);
        return ResponseEntity.noContent().build();
    }
}
