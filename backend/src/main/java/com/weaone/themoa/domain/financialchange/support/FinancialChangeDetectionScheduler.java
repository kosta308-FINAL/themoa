package com.weaone.themoa.domain.financialchange.support;

import com.weaone.themoa.domain.financialchange.service.FinancialChangeDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 관심 상품 변경 감지 스케줄러.
 *
 * <p>상품 수집(04:00)이 끝난 뒤에 돌도록 05:00로 잡는다. 수집 전에 돌면 어제와 같은 데이터를 비교하게 되어
 * 변경을 놓친다.
 */
@Component
public class FinancialChangeDetectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(FinancialChangeDetectionScheduler.class);

    private final FinancialChangeDetectionService detectionService;

    public FinancialChangeDetectionScheduler(FinancialChangeDetectionService detectionService) {
        this.detectionService = detectionService;
    }

    @Scheduled(cron = "${app.financial.change-detection.cron:0 0 5 * * *}", zone = "Asia/Seoul")
    public void runDetection() {
        try {
            detectionService.detectAll();
        } catch (RuntimeException e) {
            // 감지 실패가 다른 배치를 막지 않도록 여기서 끊는다.
            log.error("[관심상품 변경감지] 실행 실패", e);
        }
    }
}
