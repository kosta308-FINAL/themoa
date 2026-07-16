package com.weaone.themoa.domain.fixedexpense.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse;
import com.weaone.themoa.domain.fixedexpense.dto.request.FixedExpenseCandidateRegisterRequest;
import com.weaone.themoa.domain.fixedexpense.dto.request.FixedExpenseDirectRegisterRequest;
import com.weaone.themoa.domain.fixedexpense.dto.request.FixedExpenseUpdateRequest;
import com.weaone.themoa.domain.fixedexpense.dto.response.FixedExpenseListResponse;
import com.weaone.themoa.domain.fixedexpense.dto.response.FixedExpenseResponse;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseConfirmationService;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseCyclePolicy;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseDetectionService;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;

/** 고정지출 등록·조회·수정·해지(fixedExpense.md §4, F-01·F-03·F-04) + F-05 미납 확인. */
@RestController
@RequestMapping("/api/fixed-expenses")
@RequiredArgsConstructor
public class FixedExpenseController {

    private final FixedExpenseRegistrationService fixedExpenseRegistrationService;
    private final FixedExpenseConfirmationService fixedExpenseConfirmationService;
    private final FixedExpenseDetectionService fixedExpenseDetectionService;

    @Operation(summary = "고정지출 목록", description = "등록된(ACTIVE) 고정지출과 이번 달 합계를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<FixedExpenseListResponse>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        FixedExpenseListResponse response = FixedExpenseListResponse.from(fixedExpenseRegistrationService.list(memberId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "고정지출 상세", description = "고정지출 1건의 상세 정보를 조회합니다.")
    @GetMapping("/{fixedExpenseId}")
    public ResponseEntity<ApiResponse<FixedExpenseResponse>> get(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long fixedExpenseId) {
        FixedExpenseResponse response = FixedExpenseResponse.from(
                fixedExpenseRegistrationService.get(memberId, fixedExpenseId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "직접 등록", description = "탐지 후보 없이 고정지출을 직접 등록합니다(경로 B). 카드형은 가맹점(merchantAliasId 또는 newMerchantAliasName)이 필수입니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<FixedExpenseResponse>> registerDirect(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody FixedExpenseDirectRegisterRequest request) {
        FixedExpense fixedExpense = fixedExpenseRegistrationService.registerDirect(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(FixedExpenseResponse.from(fixedExpense)));
    }

    @Operation(summary = "추천 후보 승인 등록", description = "탐지된 추천 후보를 고정지출로 확정 등록합니다(경로 A). 먼저 /api/fixed-expense-candidates에서 candidateId를 확인하세요.")
    @PostMapping("/from-candidate/{candidateId}")
    public ResponseEntity<ApiResponse<FixedExpenseResponse>> registerFromCandidate(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long candidateId,
            @Valid @RequestBody FixedExpenseCandidateRegisterRequest request) {
        FixedExpense fixedExpense = fixedExpenseRegistrationService.registerFromCandidate(memberId, candidateId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(FixedExpenseResponse.from(fixedExpense)));
    }

    @Operation(summary = "고정지출 수정", description = "금액·결제일을 수정합니다.")
    @PatchMapping("/{fixedExpenseId}")
    public ResponseEntity<Void> update(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long fixedExpenseId,
            @Valid @RequestBody FixedExpenseUpdateRequest request) {
        fixedExpenseRegistrationService.update(memberId, fixedExpenseId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "고정지출 해지", description = "다음 달부터 목록·예산에서 제외합니다. 지난 달까지의 이행 기록은 남습니다.")
    @DeleteMapping("/{fixedExpenseId}")
    public ResponseEntity<Void> cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long fixedExpenseId) {
        fixedExpenseRegistrationService.cancel(memberId, fixedExpenseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 탐지 배치(새벽 03:30 cron, {@link FixedExpenseDetectionService#runNightlyDetection()})를 로그인
     * 회원 본인 범위로 즉시 실행한다. 운영 동작을 바꾸지 않는다 — 새벽 배치가 스킵하는 게 아니라
     * 그 배치가 회원별로 호출하는 것과 같은 메서드를 지금 바로 한 번 더 부르는 것뿐이라 결과는 멱등하다.
     */
    @Operation(summary = "고정지출 탐지 즉시 실행(테스트용)",
            description = "새벽 배치를 기다리지 않고 로그인 회원의 카드거래를 지금 훑어 반복결제 후보를 만듭니다. " +
                    "결과는 /api/fixed-expense-candidates에서 확인하세요.")
    @PostMapping("/detect")
    public ResponseEntity<Void> detectNow(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        fixedExpenseDetectionService.detectForMember(memberId, FixedExpenseCyclePolicy.currentYearMonth());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "미납 확인 후보 거래 목록(F-05)",
            description = "미납 알림을 탭했을 때 금액·결제일이 비슷한 미태깅 거래 후보를 조회합니다. 그 시점 기준으로 재조회하며 알림에 값을 저장해두지 않습니다.")
    @GetMapping("/{fixedExpenseId}/missed-payment-candidates")
    public ResponseEntity<ApiResponse<List<CardTransactionResponse>>> listMissedPaymentCandidates(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long fixedExpenseId) {
        List<CardTransactionResponse> response = fixedExpenseConfirmationService.listCandidates(memberId, fixedExpenseId)
                .stream()
                .map(CardTransactionResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "미납 결제 확인(F-05)",
            description = "후보 거래 중 하나를 \"이 거래예요\"로 확정합니다. 이름형은 표기를 학습하고 같은 가맹점의 과거 거래도 재태깅하며, biller 경유(Apple 등)는 결제대행사 참조를 채웁니다.")
    @PostMapping("/{fixedExpenseId}/missed-payment-candidates/{transactionId}/confirm")
    public ResponseEntity<Void> confirmMissedPayment(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long fixedExpenseId,
            @PathVariable Long transactionId) {
        fixedExpenseConfirmationService.confirm(memberId, fixedExpenseId, transactionId);
        return ResponseEntity.noContent().build();
    }
}
