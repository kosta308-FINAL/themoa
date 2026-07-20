package com.weaone.themoa.domain.category.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.category.dto.response.CategoryResponse;
import com.weaone.themoa.domain.category.service.CategoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 거래 입력·수정용 카테고리 목록(dayguide.md §8.1). */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryQueryService categoryQueryService;

    @Operation(summary = "카테고리 목록", description = "활성 전역 카테고리를 화면 표시 순서대로 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> list() {
        List<CategoryResponse> response = categoryQueryService.listAll().stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
