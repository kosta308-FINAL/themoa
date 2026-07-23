package com.weaone.themoa.domain.recommend.ingest;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * finlife 수집 배치 오케스트레이터.
 * 예적금·대출 수집을 순서대로 돌리고, 각 파트를 실패 시 재시도하며 로그를 남긴다.
 * (연금은 finlife API 장애로 현재 제외)
 */
@Service
public class CollectionBatchService {

    private static final Logger log = LoggerFactory.getLogger(CollectionBatchService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5_000L;

    private final SavingsIngestService savingsIngestService;
    private final LoanIngestService loanIngestService;

    public CollectionBatchService(SavingsIngestService savingsIngestService,
                                  LoanIngestService loanIngestService) {
        this.savingsIngestService = savingsIngestService;
        this.loanIngestService = loanIngestService;
    }

    /**
     * 수집 결과. 각 파트는 최종 실패하면 null이다(한 파트가 실패해도 다른 파트는 계속 진행하기 때문).
     * 스케줄러는 로그만 보면 되지만, 관리자 수동 실행은 화면에 건수를 보여줘야 해서 결과를 돌려준다.
     */
    public record CollectionResult(SavingsIngestService.IngestSummary savings,
                                   LoanIngestService.IngestSummary loans) {

        public boolean successful() {
            return savings != null && loans != null;
        }
    }

    /** 하루 1회 실행되는 전체 수집. 한 파트가 실패해도 다른 파트는 계속 시도한다. */
    public CollectionResult runDailyCollection() {
        return runCollection();
    }

    /** {@link #runDailyCollection()}과 같은 수집을 실행하고 집계 결과를 돌려준다(관리자 수동 실행용). */
    public CollectionResult runCollection() {
        log.info("[배치] finlife 수집 시작");
        SavingsIngestService.IngestSummary savings =
                runWithRetry("예적금", () -> savingsIngestService.ingestAll());
        LoanIngestService.IngestSummary loans =
                runWithRetry("대출", () -> loanIngestService.ingestAll());
        log.info("[배치] finlife 수집 종료");
        return new CollectionResult(savings, loans);
    }

    /**
     * task를 최대 MAX_ATTEMPTS회 시도. 실패 시 지연 후 재시도, 최종 실패는 error 로그만 남기고 null을 반환한다
     * (호출측이 다음 파트를 계속 진행할 수 있도록 예외를 밖으로 던지지 않는다).
     */
    private <T> T runWithRetry(String name, Supplier<T> task) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                T result = task.get();
                log.info("[배치] {} 성공 (시도 {}/{}): {}", name, attempt, MAX_ATTEMPTS, result);
                return result;
            } catch (Exception e) {
                log.warn("[배치] {} 실패 (시도 {}/{}): {}", name, attempt, MAX_ATTEMPTS, e.toString());
                if (attempt == MAX_ATTEMPTS) {
                    log.error("[배치] {} 최종 실패 - 이번 회차 건너뜀", name, e);
                    return null;
                }
                sleep();
            }
        }
        return null;
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
