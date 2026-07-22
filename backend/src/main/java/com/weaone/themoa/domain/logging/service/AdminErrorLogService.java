package com.weaone.themoa.domain.logging.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.logging.dto.AdminErrorLogDetailResponse;
import com.weaone.themoa.domain.logging.dto.AdminErrorLogListItemResponse;
import com.weaone.themoa.domain.logging.dto.AdminErrorLogListResponse;
import com.weaone.themoa.domain.logging.entity.AiLogDiagnosis;
import com.weaone.themoa.domain.logging.entity.ErrorLog;
import com.weaone.themoa.domain.logging.repository.AiLogDiagnosisRepository;
import com.weaone.themoa.domain.logging.repository.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 관리자 에러 목록·상세 조회(managelogging.md §5). */
@Service
@RequiredArgsConstructor
public class AdminErrorLogService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ErrorLogRepository errorLogRepository;
    private final AiLogDiagnosisRepository aiLogDiagnosisRepository;

    @Transactional(readOnly = true)
    public AdminErrorLogListResponse list(String exceptionClass, String requestUri, String controller,
                                           Long memberId, LocalDateTime startAt, LocalDateTime endAt,
                                           Integer page, Integer size) {
        Page<AdminErrorLogListItemResponse> result = errorLogRepository.searchForAdmin(
                exceptionClass, requestUri, controller, memberId, startAt, endAt,
                PageRequest.of(normalizePage(page), clampSize(size)));
        return AdminErrorLogListResponse.from(result);
    }

    @Transactional(readOnly = true)
    public AdminErrorLogDetailResponse detail(Long errorLogId) {
        ErrorLog errorLog = errorLogRepository.findById(errorLogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_ERROR_LOG_NOT_FOUND));
        AiLogDiagnosis diagnosis = aiLogDiagnosisRepository.findByErrorLog_Id(errorLogId).orElse(null);
        return AdminErrorLogDetailResponse.of(errorLog, diagnosis);
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int clampSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
