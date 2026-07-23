package com.weaone.themoa.domain.recommend.ingest;

import com.weaone.themoa.domain.datarefresh.entity.DataRefreshSource;
import com.weaone.themoa.domain.datarefresh.service.DataRefreshStatusService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * finlife 수집 배치 스케줄러.
 * 기본 매일 새벽 4시(Asia/Seoul)에 실행. finlife.batch.cron 프로퍼티로 주기 변경 가능.
 */
@Component
public class CollectionScheduler {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final CollectionBatchService batchService;
    private final DataRefreshStatusService dataRefreshStatusService;

    public CollectionScheduler(CollectionBatchService batchService,
                               DataRefreshStatusService dataRefreshStatusService) {
        this.batchService = batchService;
        this.dataRefreshStatusService = dataRefreshStatusService;
    }

    @Scheduled(cron = "${finlife.batch.cron:0 0 4 * * *}", zone = "Asia/Seoul")
    public void runScheduledCollection() {
        CollectionBatchService.CollectionResult result = batchService.runDailyCollection();
        if (result.successful()) {
            dataRefreshStatusService.recordSuccess(DataRefreshSource.FINANCIAL, LocalDateTime.now(SEOUL_ZONE));
        }
    }
}
