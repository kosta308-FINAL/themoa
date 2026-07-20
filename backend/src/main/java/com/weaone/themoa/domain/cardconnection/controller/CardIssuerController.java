package com.weaone.themoa.domain.cardconnection.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.cardconnection.dto.response.CardIssuerResponse;
import com.weaone.themoa.domain.cardconnection.service.CardIssuerQueryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** S-00C 지원 카드사 목록(dayguide.md §8.1). */
@RestController
@RequestMapping("/api/card-issuers")
@RequiredArgsConstructor
public class CardIssuerController {

    private final CardIssuerQueryService cardIssuerQueryService;

    @Operation(summary = "지원 카드사 목록",
            description = "카드 연결 화면에서 선택할 수 있는 카드사 화이트리스트를 반환합니다. requiresCardCredentials=true면 카드번호·카드 비밀번호 입력란을 추가로 보여줍니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CardIssuerResponse>>> list() {
        List<CardIssuerResponse> response = cardIssuerQueryService.listAll().stream()
                .map(CardIssuerResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
