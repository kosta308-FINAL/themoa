package com.weaone.themoa.domain.recommend.ingest;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * finlife 수집 배치 스케줄러.
 * 기본 매일 새벽 4시(Asia/Seoul)에 실행. finlife.batch.cron 프로퍼티로 주기 변경 가능.
 */
@Component
public class CollectionScheduler {

    private final CollectionBatchService batchService;

    public CollectionScheduler(CollectionBatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(cron = "${finlife.batch.cron:0 0 4 * * *}", zone = "Asia/Seoul")
    public void runScheduledCollection() {
        batchService.runDailyCollection();
    }
}
