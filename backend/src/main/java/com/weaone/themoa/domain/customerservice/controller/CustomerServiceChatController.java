package com.weaone.themoa.domain.customerservice.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.customerservice.dto.request.CustomerServiceChatRequest;
import com.weaone.themoa.domain.customerservice.dto.response.CustomerServiceChatResponse;
import com.weaone.themoa.domain.customerservice.service.CustomerServiceChatService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer-service/chat")
@RequiredArgsConstructor
public class CustomerServiceChatController {

    private final CustomerServiceChatService chatService;

    @Operation(summary = "고객센터 챗봇 질문",
            description = "고객센터 전용 지식 문서(FAQ·사용가이드·관리자 답변)를 근거로 서비스 사용법을 안내합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerServiceChatResponse>> chat(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CustomerServiceChatRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chatService.chat(memberId, request)));
    }
}
