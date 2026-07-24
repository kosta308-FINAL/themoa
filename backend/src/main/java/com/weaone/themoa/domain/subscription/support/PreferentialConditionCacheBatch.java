package com.weaone.themoa.domain.subscription.support;

import com.weaone.themoa.domain.recommend.repository.SavingsProductRepository;
import com.weaone.themoa.domain.recommend.repository.SavingsProductRepository.ProductConditionView;
import com.weaone.themoa.domain.subscription.service.PreferentialConditionCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 판매중 상품의 우대조건 파싱 캐시를 최신화하는 배치.
 *
 * <p>4시 상품 수집이 끝난 뒤 호출되어, 원문이 바뀐 상품만 재파싱한다(변경 없으면 건너뜀, 관리자
 * 수정본은 보존). 상품 단위로 트랜잭션이 나뉘어 한 건이 실패해도 나머지는 계속 진행한다.
 */
@Component
public class PreferentialConditionCacheBatch {

    private static final Logger log = LoggerFactory.getLogger(PreferentialConditionCacheBatch.class);

    private final SavingsProductRepository savingsProductRepository;
    private final PreferentialConditionCacheService cacheService;

    public PreferentialConditionCacheBatch(SavingsProductRepository savingsProductRepository,
                                           PreferentialConditionCacheService cacheService) {
        this.savingsProductRepository = savingsProductRepository;
        this.cacheService = cacheService;
    }

    /** 판매중 상품 전체를 훑어 우대조건 캐시를 최신화한다. 처리 결과(대상·실패 건수)를 돌려준다. */
    public RefreshResult refreshAll() {
        List<ProductConditionView> targets = savingsProductRepository.findSellingConditions();
        int failed = 0;
        for (ProductConditionView target : targets) {
            try {
                cacheService.refreshFromBatch(target.getId(), target.getSpecialCondition());
            } catch (Exception e) {
                failed++;
                log.warn("우대조건 캐시 갱신 실패 productId={}: {}", target.getId(), e.toString());
            }
        }
        log.info("우대조건 캐시 갱신 완료 - 대상 {}건, 실패 {}건", targets.size(), failed);
        return new RefreshResult(targets.size(), failed);
    }

    public record RefreshResult(int total, int failed) {
    }
}
