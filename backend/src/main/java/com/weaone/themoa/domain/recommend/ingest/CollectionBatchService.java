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

    /** 하루 1회 실행되는 전체 수집. 한 파트가 실패해도 다른 파트는 계속 시도한다. */
    public void runDailyCollection() {
        log.info("[배치] finlife 수집 시작");
        runWithRetry("예적금", () -> savingsIngestService.ingestAll().toString());
        runWithRetry("대출", () -> loanIngestService.ingestAll().toString());
        log.info("[배치] finlife 수집 종료");
    }

    /** task를 최대 MAX_ATTEMPTS회 시도. 실패 시 지연 후 재시도, 최종 실패는 error 로그만 남기고 넘어간다. */
    private void runWithRetry(String name, Supplier<String> task) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String result = task.get();
                log.info("[배치] {} 성공 (시도 {}/{}): {}", name, attempt, MAX_ATTEMPTS, result);
                return;
            } catch (Exception e) {
                log.warn("[배치] {} 실패 (시도 {}/{}): {}", name, attempt, MAX_ATTEMPTS, e.toString());
                if (attempt == MAX_ATTEMPTS) {
                    log.error("[배치] {} 최종 실패 - 이번 회차 건너뜀", name, e);
                    return;
                }
                sleep();
            }
        }
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
