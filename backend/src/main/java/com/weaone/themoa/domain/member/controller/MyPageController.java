package com.weaone.themoa.domain.member.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.member.dto.response.MyPageResponse;
import com.weaone.themoa.domain.member.service.MyPageQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageQueryService myPageQueryService;

    @Operation(summary = "마이페이지 요약",
            description = "회원 정보·월 저축목표·약관 동의 이력을 반환합니다. 카드 연동 현황은 /api/card-connections, "
                    + "저축목표 수정은 /api/spending-guide/savings-goal, 비밀번호 변경은 /api/auth/password를 그대로 씁니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(myPageQueryService.getMyPage(memberId)));
    }
}
