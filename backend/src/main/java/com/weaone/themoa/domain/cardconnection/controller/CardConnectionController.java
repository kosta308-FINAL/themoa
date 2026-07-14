package com.weaone.themoa.domain.cardconnection.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.cardconnection.dto.request.CardConnectionCreateRequest;
import com.weaone.themoa.domain.cardconnection.dto.response.CardConnectionResponse;
import com.weaone.themoa.domain.cardconnection.service.CardConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/card-connections")
@RequiredArgsConstructor
public class CardConnectionController {

    private final CardConnectionService cardConnectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<CardConnectionResponse>> connect(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CardConnectionCreateRequest request) {
        CardConnectionResponse response = cardConnectionService.connect(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
