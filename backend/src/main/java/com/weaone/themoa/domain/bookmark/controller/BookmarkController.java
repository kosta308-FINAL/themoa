package com.weaone.themoa.domain.bookmark.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.bookmark.dto.request.BookmarkRequest;
import com.weaone.themoa.domain.bookmark.dto.response.BookmarkResponse;
import com.weaone.themoa.domain.bookmark.dto.response.BookmarkTargetResponse;
import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;
import com.weaone.themoa.domain.bookmark.service.BookmarkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 북마크 API. 모두 로그인 사용자 본인 기준으로 동작한다(SecurityConfig의 anyRequest().authenticated()).
 *
 * <p>화면은 상품 카드의 별표를 토글하는 방식이라, 해제도 북마크 id가 아니라 "대상(타입+id)"으로 받는다.
 * 카드가 이미 들고 있는 정보만으로 등록·해제가 모두 가능해진다.
 */
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    /** 별표를 채울 때 호출. 이미 저장돼 있으면 새로 만들지 않고 200으로 응답한다. */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> register(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody BookmarkRequest request) {
        boolean created = bookmarkService.register(memberId, request);
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success());
    }

    /** 채워진 별표를 다시 누를 때 호출. */
    @DeleteMapping
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal Long memberId,
            @RequestParam BookmarkTargetType targetType,
            @RequestParam Long targetId) {
        bookmarkService.remove(memberId, targetType, targetId);
        return ResponseEntity.noContent().build();
    }

    /** 마이페이지: 저장해 둔 상품을 상세 정보와 함께 최근 순으로. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookmarkResponse>>> findAll(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.findAll(memberId)));
    }

    /** 검색·추천 화면에서 어떤 상품에 별표가 채워져야 하는지 판단할 때 쓰는 가벼운 목록. */
    @GetMapping("/targets")
    public ResponseEntity<ApiResponse<List<BookmarkTargetResponse>>> findTargets(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.findTargets(memberId)));
    }
}
