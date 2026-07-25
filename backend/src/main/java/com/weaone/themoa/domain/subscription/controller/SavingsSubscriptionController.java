package com.weaone.themoa.domain.subscription.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.subscription.dto.request.ConditionMetUpdateRequest;
import com.weaone.themoa.domain.subscription.dto.request.SubscriptionCreateRequest;
import com.weaone.themoa.domain.subscription.dto.request.SubscriptionUpdateRequest;
import com.weaone.themoa.domain.subscription.dto.response.SubscriptionDraftResponse;
import com.weaone.themoa.domain.subscription.dto.response.SubscriptionResponse;
import com.weaone.themoa.domain.subscription.service.SavingsSubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * 사용자가 가입한 예·적금 등록·조회. 모두 로그인 사용자 본인 기준으로 동작한다.
 */
@RestController
@RequestMapping("/api/savings-subscriptions")
public class SavingsSubscriptionController {

    private final SavingsSubscriptionService subscriptionService;

    public SavingsSubscriptionController(SavingsSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /** 상품을 골랐을 때 가입 등록 폼 초안(금리 범위 + 우대조건 항목). */
    @GetMapping("/draft")
    public ResponseEntity<ApiResponse<SubscriptionDraftResponse>> draft(@RequestParam Long productId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.draftFromProduct(productId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody SubscriptionCreateRequest request) {
        Long id = subscriptionService.create(memberId, request);
        return ResponseEntity.created(URI.create("/api/savings-subscriptions/" + id))
                .body(ApiResponse.success(id));
    }

    /** 대시보드: 가입한 상품 목록(예상 만기금액 포함). */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> findAll(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.findAll(memberId)));
    }

    /** 우대조건 충족 여부 토글. */
    @PatchMapping("/conditions/{conditionId}")
    public ResponseEntity<Void> updateConditionMet(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long conditionId,
            @Valid @RequestBody ConditionMetUpdateRequest request) {
        subscriptionService.updateConditionMet(memberId, conditionId, request.met());
        return ResponseEntity.noContent().build();
    }

    /** 가입 정보 수정(월납입·적용금리·기간·가입일). 우대조건 토글은 별도 PATCH 사용. */
    @PutMapping("/{subscriptionId}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long subscriptionId,
            @Valid @RequestBody SubscriptionUpdateRequest request) {
        subscriptionService.update(memberId, subscriptionId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long subscriptionId) {
        subscriptionService.delete(memberId, subscriptionId);
        return ResponseEntity.noContent().build();
    }
}
