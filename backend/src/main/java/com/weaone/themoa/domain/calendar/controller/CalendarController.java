package com.weaone.themoa.domain.calendar.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.calendar.dto.request.CalendarScheduleCreateRequest;
import com.weaone.themoa.domain.calendar.dto.request.CalendarScheduleUpdateRequest;
import com.weaone.themoa.domain.calendar.dto.response.CalendarEventListResponse;
import com.weaone.themoa.domain.calendar.dto.response.CalendarScheduleResponse;
import com.weaone.themoa.domain.calendar.service.CalendarQueryService;
import com.weaone.themoa.domain.calendar.service.CalendarScheduleService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {
    private final CalendarQueryService queryService;
    private final CalendarScheduleService scheduleService;

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<CalendarEventListResponse>> events(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(queryService.getEvents(memberId, startDate, endDate)));
    }

    @PostMapping("/schedules")
    public ResponseEntity<ApiResponse<CalendarScheduleResponse>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CalendarScheduleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(scheduleService.create(memberId, request)));
    }

    @PatchMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<CalendarScheduleResponse>> update(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody CalendarScheduleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.update(memberId, scheduleId, request)));
    }

    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long scheduleId) {
        scheduleService.delete(memberId, scheduleId);
        return ResponseEntity.noContent().build();
    }
}
