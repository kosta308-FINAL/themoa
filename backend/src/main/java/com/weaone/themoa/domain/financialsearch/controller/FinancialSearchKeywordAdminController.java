package com.weaone.themoa.domain.financialsearch.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.financialsearch.dto.request.FinancialSearchKeywordAddRequest;
import com.weaone.themoa.domain.financialsearch.dto.response.FinancialSearchKeywordListResponse;
import com.weaone.themoa.domain.financialsearch.service.FinancialSearchKeywordAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 검색어 해석 키워드 관리(관리자 전용).
 *
 * <p>인구집단 키워드(청년·시니어 등)와 상품의도 키워드(적금·대출 등)를 화면에서 편집하면 재배포 없이
 * 검색 동작이 바뀐다. 예: SENIOR 그룹에 "노후"를 추가하면 "노후 준비" 검색에 시니어 상품이 잡힌다.
 *
 * <p>경로가 {@code /api/admin/**} 아래라 SecurityConfig의 기존 규칙으로 ADMIN 권한이 적용된다.
 */
@RestController
@RequestMapping("/api/admin/financial-products/search/keywords")
public class FinancialSearchKeywordAdminController {

    private final FinancialSearchKeywordAdminService keywordAdminService;

    public FinancialSearchKeywordAdminController(FinancialSearchKeywordAdminService keywordAdminService) {
        this.keywordAdminService = keywordAdminService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<FinancialSearchKeywordListResponse>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(keywordAdminService.findAll()));
    }

    /** 키워드 추가. 이미 있는 단어면 새로 만들지 않고 200으로 응답한다. */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> add(@Valid @RequestBody FinancialSearchKeywordAddRequest request) {
        boolean created = keywordAdminService.add(request);
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success());
    }

    @DeleteMapping("/{keywordId}")
    public ResponseEntity<Void> delete(@PathVariable Long keywordId) {
        keywordAdminService.delete(keywordId);
        return ResponseEntity.noContent().build();
    }

    /** 기본값으로 초기화. 편집한 내용을 모두 지우고 기본 키워드로 되돌린다. */
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<FinancialSearchKeywordListResponse>> reset() {
        return ResponseEntity.ok(ApiResponse.success(keywordAdminService.resetToDefaults()));
    }
}
