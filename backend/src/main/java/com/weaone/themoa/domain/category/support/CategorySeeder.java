package com.weaone.themoa.domain.category.support;

import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.entity.CategoryKeywordRule;
import com.weaone.themoa.domain.category.entity.MerchantTypeCategoryMap;
import com.weaone.themoa.domain.category.repository.CategoryKeywordRuleRepository;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.category.repository.MerchantTypeCategoryMapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카테고리 전역 마스터 시드(category.md §3~§5). {@link com.weaone.themoa.domain.merchant.support.MerchantAliasSeeder}가
 * 카테고리 FK를 참조하므로 반드시 그보다 먼저 실행돼야 한다({@link Order}).
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class CategorySeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final MerchantTypeCategoryMapRepository merchantTypeCategoryMapRepository;
    private final CategoryKeywordRuleRepository categoryKeywordRuleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<CategoryCode, Category> categories = seedCategories();
        seedMerchantTypeMap(categories);
        seedKeywordRules(categories);
    }

    /**
     * 코드별로 없는 것만 추가한다. 기존엔 "테이블이 비어 있을 때만 통째로 시드"였는데, 그러면 이미
     * 시드가 끝난 운영/개발 DB에 새 카테고리(예: SAVING)를 추가해도 절대 반영되지 않는다.
     */
    private Map<CategoryCode, Category> seedCategories() {
        Map<CategoryCode, Category> existing = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(c -> CategoryCode.valueOf(c.getCode()), c -> c));

        record Seed(CategoryCode code, String name) {
        }
        List<Seed> desired = List.of(
                new Seed(CategoryCode.FOOD, "식비"),
                new Seed(CategoryCode.DELIVERY, "배달"),
                new Seed(CategoryCode.CAFE, "카페"),
                new Seed(CategoryCode.CONVENIENCE, "편의점/마트"),
                new Seed(CategoryCode.TRANSPORT, "교통"),
                new Seed(CategoryCode.SHOPPING, "쇼핑"),
                new Seed(CategoryCode.SUBSCRIPTION, "구독"),
                new Seed(CategoryCode.LEISURE, "여가"),
                new Seed(CategoryCode.MEDICAL, "의료"),
                new Seed(CategoryCode.BEAUTY, "미용"),
                new Seed(CategoryCode.DONATION, "기부/회비"),
                new Seed(CategoryCode.SAVING, "저축"),
                new Seed(CategoryCode.ETC, "기타")
        );

        List<Category> missing = desired.stream()
                .filter(seed -> !existing.containsKey(seed.code()))
                .map(seed -> Category.seed(seed.code(), seed.name()))
                .toList();
        if (!missing.isEmpty()) {
            categoryRepository.saveAll(missing).forEach(c -> existing.put(CategoryCode.valueOf(c.getCode()), c));
        }
        return existing;
    }

    /**
     * 없는 것만 추가한다(seedCategories와 동일한 이유 — 이미 시드가 끝난 DB에도 새 매핑이 반영돼야 한다).
     * 2026-07 실제 CODEF 승인내역 2건(약 940건, hoyeonCodefApiResponse/mycodef) 분석으로 발견된 업종을
     * 추가했다. 결제대행/오픈마켓류(전자상거래PG, 전자상거래오픈마켓, 기타4 등)는 같은 업종 안에 서로 무관한
     * 가맹점이 뒤섞여 있어(예: 네이버페이·배민·CJ CGV가 전부 "전자상거래PG") 업종 매핑 대상에서 뺐다 —
     * 그런 건 가맹점명 키워드({@link #seedKeywordRules})로만 판별한다.
     */
    private void seedMerchantTypeMap(Map<CategoryCode, Category> categories) {
        record Seed(String merchantType, CategoryCode code) {
        }
        List<Seed> desired = List.of(
                new Seed("편의점", CategoryCode.CONVENIENCE),
                new Seed("할인점/슈퍼마켓", CategoryCode.CONVENIENCE),
                new Seed("식품잡화", CategoryCode.CONVENIENCE),
                new Seed("한식", CategoryCode.FOOD),
                new Seed("일반대중음식", CategoryCode.FOOD),
                new Seed("패스트푸드", CategoryCode.FOOD),
                new Seed("중식", CategoryCode.FOOD),
                new Seed("일식", CategoryCode.FOOD),
                new Seed("커피전문점", CategoryCode.CAFE),
                new Seed("택시", CategoryCode.TRANSPORT),
                new Seed("노래방", CategoryCode.LEISURE),
                new Seed("PC게임방", CategoryCode.LEISURE),
                new Seed("스포츠센타/레포츠클럽", CategoryCode.LEISURE),
                new Seed("약국", CategoryCode.MEDICAL),
                new Seed("이용,미용", CategoryCode.BEAUTY),
                new Seed("각종회비", CategoryCode.DONATION),
                new Seed("컴퓨터  소프트웨어", CategoryCode.SUBSCRIPTION),
                new Seed("화원", CategoryCode.SHOPPING),
                // 아래부터 2026-07 실데이터 분석 추가분
                new Seed("주유소", CategoryCode.TRANSPORT),
                new Seed("패스트푸드점", CategoryCode.FOOD),
                new Seed("일반음식점 기타", CategoryCode.FOOD),
                new Seed("일식/생선회집", CategoryCode.FOOD),
                new Seed("양식", CategoryCode.FOOD),
                new Seed("패밀리레스토랑", CategoryCode.FOOD),
                new Seed("휴게음식점", CategoryCode.FOOD),
                new Seed("일반주점", CategoryCode.FOOD),
                new Seed("대형마트", CategoryCode.CONVENIENCE),
                new Seed("슈퍼마켓", CategoryCode.CONVENIENCE),
                new Seed("농.수.축산물점", CategoryCode.CONVENIENCE),
                new Seed("농.수.축협직판장", CategoryCode.CONVENIENCE),
                new Seed("기타 식품", CategoryCode.CONVENIENCE),
                new Seed("자동판매기 운영업", CategoryCode.CONVENIENCE),
                new Seed("일반잡화판매점", CategoryCode.SHOPPING),
                new Seed("서점", CategoryCode.SHOPPING),
                new Seed("문방구점", CategoryCode.SHOPPING),
                new Seed("악세사리점", CategoryCode.SHOPPING),
                new Seed("백화점", CategoryCode.SHOPPING),
                new Seed("화장품점", CategoryCode.BEAUTY),
                new Seed("요가", CategoryCode.LEISURE),
                new Seed("볼링장", CategoryCode.LEISURE),
                new Seed("비디오방/게임방", CategoryCode.LEISURE),
                new Seed("찜질방/목욕탕", CategoryCode.LEISURE),
                new Seed("커피/음료전문점", CategoryCode.CAFE),
                new Seed("제과점/아이스크림점", CategoryCode.CAFE),
                new Seed("일반.치과.한의원", CategoryCode.MEDICAL),
                new Seed("RF대중교통", CategoryCode.TRANSPORT),
                new Seed("RF유료도로.터널", CategoryCode.TRANSPORT),
                new Seed("고속.시외버스", CategoryCode.TRANSPORT),
                new Seed("철도", CategoryCode.TRANSPORT),
                new Seed("주차장", CategoryCode.TRANSPORT),
                new Seed("우체국(우편요금)", CategoryCode.ETC),
                new Seed("이벤트업", CategoryCode.ETC),
                new Seed("기타 용역서비스", CategoryCode.ETC),
                new Seed("기타 수리서비스", CategoryCode.ETC),
                new Seed("공공기관직영점", CategoryCode.ETC),
                new Seed("독서실", CategoryCode.ETC),
                new Seed("RF유통기관", CategoryCode.ETC)
        );

        List<MerchantTypeCategoryMap> missing = desired.stream()
                .filter(seed -> !merchantTypeCategoryMapRepository.existsByMerchantType(seed.merchantType()))
                .map(seed -> MerchantTypeCategoryMap.seed(seed.merchantType(), categories.get(seed.code())))
                .toList();
        if (!missing.isEmpty()) {
            merchantTypeCategoryMapRepository.saveAll(missing);
        }
    }

    /**
     * 없는 것만 추가한다(위와 동일한 이유). 2026-07 실데이터 분석 추가분: 우아한형제들(배민)·CJ CGV·
     * 티켓링크류(문화상품권 결제)·맥도날드·그린카·야놀자·무신사처럼 결제대행 업종 뒤에 숨는 가맹점,
     * 쿠팡(와우 멤버십)의 표기 변형, OPENAI(ChatGPT) 구독을 새로 추가했다.
     */
    private void seedKeywordRules(Map<CategoryCode, Category> categories) {
        record Seed(String keyword, CategoryCode code) {
        }
        List<Seed> desired = List.of(
                new Seed("쿠팡(쿠페이)", CategoryCode.SUBSCRIPTION),
                new Seed("쿠팡이츠", CategoryCode.DELIVERY),
                new Seed("쿠팡", CategoryCode.SHOPPING),
                new Seed("쏘카", CategoryCode.TRANSPORT),
                new Seed("카카오T", CategoryCode.TRANSPORT),
                new Seed("카카오모빌리티", CategoryCode.TRANSPORT),
                new Seed("나인투원(킥보드)", CategoryCode.TRANSPORT),
                new Seed("피플카쉐어링", CategoryCode.TRANSPORT),
                new Seed("ANTHROPIC", CategoryCode.SUBSCRIPTION),
                new Seed("CLAUDE", CategoryCode.SUBSCRIPTION),
                new Seed("구글클라우드", CategoryCode.SUBSCRIPTION),
                new Seed("AWS", CategoryCode.SUBSCRIPTION),
                new Seed("Amazon", CategoryCode.SUBSCRIPTION),
                new Seed("PC CAFE", CategoryCode.LEISURE),
                new Seed("긱스타", CategoryCode.LEISURE),
                new Seed("의원", CategoryCode.MEDICAL),
                // 아래부터 2026-07 실데이터 분석 추가분
                new Seed("쿠팡(와우", CategoryCode.SUBSCRIPTION),
                new Seed("쿠팡플레이", CategoryCode.SUBSCRIPTION),
                new Seed("우아한형제들", CategoryCode.DELIVERY),
                new Seed("네이버플러스 멤버십", CategoryCode.SUBSCRIPTION),
                new Seed("맥도날드", CategoryCode.FOOD),
                new Seed("CJ CGV", CategoryCode.LEISURE),
                new Seed("티켓링크", CategoryCode.LEISURE),
                new Seed("멜론티켓", CategoryCode.LEISURE),
                new Seed("놀유니버스", CategoryCode.LEISURE),
                new Seed("문화비", CategoryCode.LEISURE),
                new Seed("야놀자", CategoryCode.LEISURE),
                new Seed("그린카", CategoryCode.TRANSPORT),
                new Seed("무신사", CategoryCode.SHOPPING),
                new Seed("OPENAI", CategoryCode.SUBSCRIPTION),
                new Seed("Steam", CategoryCode.LEISURE),
                new Seed("AIRBNB", CategoryCode.LEISURE),
                new Seed("카카오 T", CategoryCode.TRANSPORT),
                new Seed("Apple", CategoryCode.SUBSCRIPTION),
                new Seed("Google Play", CategoryCode.SUBSCRIPTION)
        );

        List<CategoryKeywordRule> missing = desired.stream()
                .filter(seed -> !categoryKeywordRuleRepository.existsByKeyword(seed.keyword()))
                .map(seed -> CategoryKeywordRule.seed(seed.keyword(), categories.get(seed.code())))
                .toList();
        if (!missing.isEmpty()) {
            categoryKeywordRuleRepository.saveAll(missing);
        }
    }
}
