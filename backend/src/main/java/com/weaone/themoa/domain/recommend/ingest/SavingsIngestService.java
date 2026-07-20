package com.weaone.themoa.domain.recommend.ingest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import com.weaone.themoa.domain.recommend.entity.SavingsProductOption;
import com.weaone.themoa.domain.recommend.repository.SavingsProductRepository;
import com.weaone.themoa.domain.recommend.entity.SavingsType;
import com.weaone.themoa.domain.recommend.finlife.FinlifeClient;
import com.weaone.themoa.domain.recommend.finlife.FinlifeClient.CollectResult;
import com.weaone.themoa.domain.recommend.dto.SavingsBaseItem;
import com.weaone.themoa.domain.recommend.dto.SavingsOptionItem;

/**
 * 예·적금 수집→저장 서비스 (배치 4단계 중 예적금 파트).
 * 흐름: finlife 호출 → 판매중(close_date 없음)만 남김 → base·option 매칭 → 회사코드+상품코드+유형 기준 Upsert.
 */
@Service
public class SavingsIngestService {

    /** 수집 대상 권역: 020000 은행, 030300 저축은행. */
    private static final List<String> GROUPS = List.of("020000", "030300");

    private final FinlifeClient finlifeClient;
    private final SavingsProductRepository repository;

    public SavingsIngestService(FinlifeClient finlifeClient, SavingsProductRepository repository) {
        this.finlifeClient = finlifeClient;
        this.repository = repository;
    }

    /** 예금+적금을 은행/저축은행 권역별로 모두 수집·저장하고 집계를 돌려준다. */
    @Transactional
    public IngestSummary ingestAll() {
        IngestSummary summary = new IngestSummary();
        for (String group : GROUPS) {
            ingest(finlifeClient.fetchDeposits(group), SavingsType.DEPOSIT, summary);
            ingest(finlifeClient.fetchSavings(group), SavingsType.SAVING, summary);
        }
        return summary;
    }

    private void ingest(CollectResult<SavingsBaseItem, SavingsOptionItem> result,
                        SavingsType type, IngestSummary summary) {

        // 옵션을 (회사코드+상품코드) 기준으로 묶어둔다 → 각 상품이 자기 옵션을 빠르게 찾도록.
        Map<String, List<SavingsOptionItem>> optionsByKey = result.optionList().stream()
                .collect(Collectors.groupingBy(o -> key(o.finCoNo(), o.finPrdtCd())));

        for (SavingsBaseItem base : result.baseList()) {
            summary.fetched++;

            // 판매종료 상품(close_date 있음)은 저장하지 않는다.
            if (isClosed(base.dclsEndDay())) {
                summary.skippedClosed++;
                continue;
            }

            List<SavingsProductOption> options = optionsByKey
                    .getOrDefault(key(base.finCoNo(), base.finPrdtCd()), List.of())
                    .stream()
                    .map(SavingsMapper::toOption)
                    .toList();

            upsert(base, type, options, summary);
        }
    }

    private void upsert(SavingsBaseItem base, SavingsType type,
                        List<SavingsProductOption> options, IngestSummary summary) {

        // 가입대상 파싱 + 자동 태깅 (하드필터/추천에 쓸 가공 컬럼)
        var parsed = JoinTargetParser.parse(base.joinMember());
        boolean lowIncome = "2".equals(base.joinDeny()) || Boolean.TRUE.equals(parsed.isForLowIncome());
        Boolean online = SavingsTagger.isOnline(base.joinWay());
        String difficulty = SavingsTagger.difficulty(base.spclCnd());
        Boolean youthFriendly = SavingsTagger.youthFriendly(
                base.finPrdtNm(), base.joinMember(), parsed.minAge(), parsed.maxAge());

        repository.findByCompanyCodeAndProductCodeAndProductType(base.finCoNo(), base.finPrdtCd(), type)
                .ifPresentOrElse(existing -> {
                    // 이미 있으면 기본정보 갱신 + 옵션 통째로 교체
                    existing.updateBasicInfo(base.korCoNm(), base.finPrdtNm(), base.joinWay(),
                            base.joinDeny(), base.joinMember(), base.spclCnd(), base.mtrtInt(),
                            base.etcNote(), SavingsMapper.toIntOrNull(base.maxLimit()),
                            base.dclsStrtDay(), base.dclsEndDay());
                    existing.replaceOptions(options);
                    existing.applyParsedCondition(parsed.minAge(), parsed.maxAge(), parsed.incomeLimit(),
                            parsed.incomeMin(), parsed.employmentType(), lowIncome);
                    existing.applyTags(online, difficulty, youthFriendly);
                    summary.updated++;
                }, () -> {
                    // 없으면 신규 저장(옵션은 cascade로 함께 저장)
                    SavingsProduct product = SavingsMapper.toProduct(base, type);
                    options.forEach(product::addOption);
                    product.applyParsedCondition(parsed.minAge(), parsed.maxAge(), parsed.incomeLimit(),
                            parsed.incomeMin(), parsed.employmentType(), lowIncome);
                    product.applyTags(online, difficulty, youthFriendly);
                    repository.save(product);
                    summary.inserted++;
                });
    }

    private static boolean isClosed(String dclsEndDay) {
        return dclsEndDay != null && !dclsEndDay.isBlank();
    }

    private static String key(String companyCode, String productCode) {
        return companyCode + "|" + productCode;
    }

    /** 수집 집계 결과. */
    public static class IngestSummary {
        public int fetched;        // finlife에서 받은 총 상품 수
        public int skippedClosed;  // 판매종료로 제외
        public int inserted;       // 신규 저장
        public int updated;        // 기존 갱신

        public int saved() {
            return inserted + updated;
        }

        @Override
        public String toString() {
            return "받음 " + fetched + " / 판매종료제외 " + skippedClosed
                    + " / 신규 " + inserted + " / 갱신 " + updated + " (저장합계 " + saved() + ")";
        }
    }
}
