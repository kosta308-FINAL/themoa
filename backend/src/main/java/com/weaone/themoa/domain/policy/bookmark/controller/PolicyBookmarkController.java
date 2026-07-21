package com.weaone.themoa.domain.policy.bookmark.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.policy.bookmark.dto.response.PolicyBookmarkListResponse;
import com.weaone.themoa.domain.policy.bookmark.dto.response.PolicyBookmarkResponse;
import com.weaone.themoa.domain.policy.bookmark.service.PolicyBookmarkService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies/bookmarks")
@RequiredArgsConstructor
public class PolicyBookmarkController {
    private final PolicyBookmarkService bookmarkService;

    @GetMapping
    public ResponseEntity<ApiResponse<PolicyBookmarkListResponse>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.list(memberId)));
    }

    @PostMapping("/{policyId}")
    public ResponseEntity<ApiResponse<PolicyBookmarkResponse>> add(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Integer policyId) {
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.add(memberId, policyId)));
    }

    @DeleteMapping("/{policyId}")
    public ResponseEntity<Void> remove(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Integer policyId) {
        bookmarkService.remove(memberId, policyId);
        return ResponseEntity.noContent().build();
    }
}
