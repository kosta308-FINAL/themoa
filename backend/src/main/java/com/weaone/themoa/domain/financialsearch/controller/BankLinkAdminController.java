package com.weaone.themoa.domain.financialsearch.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.financialsearch.dto.request.BankLinkSaveRequest;
import com.weaone.themoa.domain.financialsearch.dto.response.BankLinkListResponse;
import com.weaone.themoa.domain.financialsearch.service.BankLinkAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 은행 공식 홈페이지 링크 관리(관리자 전용).
 *
 * <p>링크가 등록되지 않은 금융회사의 상품은 검색·북마크 결과에서 공식 홈페이지 대신 검색 링크로 나간다.
 * 검증된 URL만 등록해야 한다 — 확인되지 않은 주소를 넣으면 사용자를 엉뚱한 곳으로 보내게 된다.
 *
 * <p>경로가 {@code /api/admin/**} 아래라 SecurityConfig의 기존 규칙으로 ADMIN 권한이 적용된다.
 */
@RestController
@RequestMapping("/api/admin/financial-products/bank-links")
public class BankLinkAdminController {

    private final BankLinkAdminService bankLinkAdminService;

    public BankLinkAdminController(BankLinkAdminService bankLinkAdminService) {
        this.bankLinkAdminService = bankLinkAdminService;
    }

    /** 등록된 링크 목록 + 아직 링크가 없는 금융회사 목록. */
    @GetMapping
    public ResponseEntity<ApiResponse<BankLinkListResponse>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(bankLinkAdminService.findAll()));
    }

    /** 등록 또는 수정(회사명이 식별자). 새로 만들면 201, 기존 URL을 바꾸면 200. */
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> save(@Valid @RequestBody BankLinkSaveRequest request) {
        boolean created = bankLinkAdminService.save(request);
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success());
    }

    @DeleteMapping("/{companyName}")
    public ResponseEntity<Void> delete(@PathVariable String companyName) {
        bankLinkAdminService.delete(companyName);
        return ResponseEntity.noContent().build();
    }
}
