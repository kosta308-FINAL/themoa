package com.weaone.themoa.domain.logging.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.logging.dto.AiDiagnosisRequestResponse;
import com.weaone.themoa.domain.logging.entity.AiDiagnosisStatus;
import com.weaone.themoa.domain.logging.entity.AiLogDiagnosis;
import com.weaone.themoa.domain.logging.entity.ErrorLog;
import com.weaone.themoa.domain.logging.repository.AiLogDiagnosisRepository;
import com.weaone.themoa.domain.logging.repository.ErrorLogRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * PENDING 행 생성/초기화, 중복 실행 방지, 비동기 작업 제출(managelogging.md §5-4, §6-4). worker 제출은
 * 트랜잭션이 실제로 커밋된 뒤에만 실행한다 — {@code saveAndFlush}만으로는 다른 커넥션(worker 스레드)이
 * 아직 커밋 전인 PENDING 행을 보지 못하는 경합이 남기 때문이다.
 */
@Slf4j
@Service
public class AiDiagnosisCommandService {

    private final ErrorLogRepository errorLogRepository;
    private final AiLogDiagnosisRepository aiLogDiagnosisRepository;
    private final MemberRepository memberRepository;
    private final AiDiagnosisWorker aiDiagnosisWorker;
    private final TaskExecutor aiDiagnosisTaskExecutor;

    public AiDiagnosisCommandService(ErrorLogRepository errorLogRepository,
                                      AiLogDiagnosisRepository aiLogDiagnosisRepository,
                                      MemberRepository memberRepository,
                                      AiDiagnosisWorker aiDiagnosisWorker,
                                      @Qualifier("aiDiagnosisTaskExecutor") TaskExecutor aiDiagnosisTaskExecutor) {
        this.errorLogRepository = errorLogRepository;
        this.aiLogDiagnosisRepository = aiLogDiagnosisRepository;
        this.memberRepository = memberRepository;
        this.aiDiagnosisWorker = aiDiagnosisWorker;
        this.aiDiagnosisTaskExecutor = aiDiagnosisTaskExecutor;
    }

    @Transactional
    public AiDiagnosisRequestResponse requestAnalysis(Long errorLogId, Long requestedByMemberId) {
        ErrorLog errorLog = errorLogRepository.findById(errorLogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_ERROR_LOG_NOT_FOUND));

        AiLogDiagnosis diagnosis = aiLogDiagnosisRepository.findByErrorLog_Id(errorLogId).orElse(null);
        Member requestedBy = requestedByMemberId == null ? null : memberRepository.getReferenceById(requestedByMemberId);
        LocalDateTime now = LocalDateTime.now();

        boolean shouldSubmit;
        if (diagnosis == null) {
            diagnosis = AiLogDiagnosis.createPending(errorLog, requestedBy, AiDiagnosisWorker.modelName(), now);
            shouldSubmit = true;
        } else if (diagnosis.getStatus() == AiDiagnosisStatus.PENDING) {
            // 이미 PENDING이면 새 작업을 만들지 않고 기존 PENDING 응답을 반환한다(§5-4).
            shouldSubmit = false;
        } else {
            diagnosis.resetToPending(requestedBy, now);
            shouldSubmit = true;
        }

        AiLogDiagnosis saved = aiLogDiagnosisRepository.saveAndFlush(diagnosis);
        Long diagnosisId = saved.getId();

        if (shouldSubmit) {
            submitAfterCommit(diagnosisId);
        }
        return new AiDiagnosisRequestResponse(diagnosisId, errorLogId, saved.getStatus());
    }

    private void submitAfterCommit(Long diagnosisId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            submitWorker(diagnosisId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                submitWorker(diagnosisId);
            }
        });
    }

    private void submitWorker(Long diagnosisId) {
        try {
            aiDiagnosisTaskExecutor.execute(() -> aiDiagnosisWorker.diagnose(diagnosisId));
        } catch (TaskRejectedException e) {
            log.error("AI 진단 작업 제출이 거부되었습니다. diagnosisId={}", diagnosisId, e);
            markRejected(diagnosisId);
        }
    }

    private void markRejected(Long diagnosisId) {
        aiLogDiagnosisRepository.findById(diagnosisId).ifPresent(diagnosis -> {
            diagnosis.fail("분석 작업 큐가 가득 차 요청을 처리하지 못했습니다.", LocalDateTime.now());
            aiLogDiagnosisRepository.save(diagnosis);
        });
    }
}
