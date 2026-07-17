package com.weaone.themoa.domain.cardconnection.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.cardconnection.dto.request.CardConnectionCreateRequest;
import com.weaone.themoa.domain.cardconnection.dto.request.CardSyncSettingRequest;
import com.weaone.themoa.domain.cardconnection.dto.response.CardConnectionListResponse;
import com.weaone.themoa.domain.cardconnection.dto.response.CardConnectionResponse;
import com.weaone.themoa.domain.cardconnection.service.CardConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    /** 카드 자동수집 ON/OFF(entryMode.md §2-1). OFF→ON 전환은 서비스 계층이 갭 백필을 트리거한다(§4-1). */
    @PatchMapping("/card-sync")
    public ResponseEntity<Void> setCardSync(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CardSyncSettingRequest request) {
        cardConnectionService.setCardSyncEnabled(memberId, request.enabled());
        return ResponseEntity.noContent().build();
    }
}
