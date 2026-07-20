package com.weaone.themoa.domain.customerservice.support;

import com.weaone.themoa.domain.customerservice.entity.Faq;
import com.weaone.themoa.domain.customerservice.entity.FaqCategory;
import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryCategory;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryCategoryRepository;
import com.weaone.themoa.domain.customerservice.repository.FaqCategoryRepository;
import com.weaone.themoa.domain.customerservice.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 고객센터 FAQ·문의 카테고리 마스터 시드(customerservice.md, erd.md §8). 서비스의 실제 기능(카드 연동,
 * 소비 가이드, 고정지출, 수기 입력, 계정 보안, 정책·금융상품)을 기준으로 사용자가 실제로 물어볼 만한
 * 질문을 채운다.
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class CustomerServiceMasterSeeder implements ApplicationRunner {

    private final FaqCategoryRepository faqCategoryRepository;
    private final FaqRepository faqRepository;
    private final CustomerInquiryCategoryRepository inquiryCategoryRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedInquiryCategories();
        seedFaq();
    }

    private void seedInquiryCategories() {
        if (inquiryCategoryRepository.count() > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE customer_inquiry_category AUTO_INCREMENT = 1");
        LocalDateTime now = LocalDateTime.now();
        inquiryCategoryRepository.saveAll(List.of(
                CustomerInquiryCategory.seed("카드 연동", 1, now),
                CustomerInquiryCategory.seed("일일 예산", 2, now),
                CustomerInquiryCategory.seed("고정지출", 3, now),
                CustomerInquiryCategory.seed("지출 직접 입력", 4, now),
                CustomerInquiryCategory.seed("계정·보안", 5, now),
                CustomerInquiryCategory.seed("정책", 6, now),
                CustomerInquiryCategory.seed("금융상품", 7, now),
                CustomerInquiryCategory.seed("기타", 8, now)
        ));
    }

    private void seedFaq() {
        if (faqCategoryRepository.count() > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE faq_category AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE faq AUTO_INCREMENT = 1");
        LocalDateTime now = LocalDateTime.now();
        Map<String, FaqCategory> categories = faqCategoryRepository.saveAll(List.of(
                FaqCategory.seed("CARD_SYNC", "카드 연동/수집", 1, now),
                FaqCategory.seed("DAILY_BUDGET", "소비 가이드/하루 권장액", 2, now),
                FaqCategory.seed("FIXED_EXPENSE", "고정지출/구독 관리", 3, now),
                FaqCategory.seed("MANUAL_EXPENSE", "수기 지출/입력모드", 4, now),
                FaqCategory.seed("ACCOUNT_SECURITY", "계정/보안", 5, now),
                FaqCategory.seed("POLICY_PRODUCT", "정책/금융상품 추천", 6, now)
        )).stream().collect(Collectors.toMap(FaqCategory::getCode, c -> c));

        faqRepository.saveAll(List.of(
                cardSyncNotSyncing(categories.get("CARD_SYNC"), now),
                cardSyncPasswordLock(categories.get("CARD_SYNC"), now),
                cardSyncBackfill(categories.get("CARD_SYNC"), now),
                cardSyncMultipleIssuers(categories.get("CARD_SYNC"), now),

                dailyBudgetFormula(categories.get("DAILY_BUDGET"), now),
                dailyBudgetMidCycleChange(categories.get("DAILY_BUDGET"), now),
                dailyBudgetPaydayChange(categories.get("DAILY_BUDGET"), now),
                dailyBudgetSurplus(categories.get("DAILY_BUDGET"), now),

                fixedExpenseDoubleCount(categories.get("FIXED_EXPENSE"), now),
                fixedExpenseAutoDetect(categories.get("FIXED_EXPENSE"), now),
                fixedExpenseExchangeRate(categories.get("FIXED_EXPENSE"), now),
                fixedExpenseAmountChange(categories.get("FIXED_EXPENSE"), now),

                manualExpenseCashTransfer(categories.get("MANUAL_EXPENSE"), now),
                manualExpenseCardSyncLater(categories.get("MANUAL_EXPENSE"), now),
                manualExpenseWhileSyncing(categories.get("MANUAL_EXPENSE"), now),

                accountPasswordChange(categories.get("ACCOUNT_SECURITY"), now),
                accountLoginLock(categories.get("ACCOUNT_SECURITY"), now),
                accountMultiDeviceLogout(categories.get("ACCOUNT_SECURITY"), now),

                policyRecommendBasis(categories.get("POLICY_PRODUCT"), now),
                policyRegionLater(categories.get("POLICY_PRODUCT"), now)
        ));
    }

    private Faq cardSyncNotSyncing(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "카드가 제대로 연동되지 않거나 최근 결제 내역이 안 불러와져요.", """
                더모아는 외부 연동(CODEF API)을 통해 매일 새벽 및 앱 진입 시 최근 결제 내역을 불러옵니다.

                아래와 같은 경우 수집이 지연되거나 중단될 수 있습니다.

                - 카드사 비밀번호가 변경되었거나 로그인 세션이 만료된 경우
                - 카드사 비밀번호를 3회 이상 잘못 입력하여 일시적으로 연결이 제한된 경우
                - 카드사 서버 점검 시간

                **해결 방법:** [카드/소비내역] 또는 [설정] 메뉴에서 해당 카드사의 '연결 재시도' 버튼을 눌러 자격 증명을 갱신해 보세요.
                """, 100, now);
    }

    private Faq cardSyncPasswordLock(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "카드사 비밀번호를 여러 번 잘못 입력했더니 연결이 안 돼요.", """
                더모아는 회원님의 카드사 계정이 실제로 잠기기 전에 먼저 연결을 막아 보호합니다.

                - 비밀번호를 **3회 연속 잘못 입력**하면 더모아가 재시도를 **5분간 차단**합니다. 5분이 지나거나 이후 로그인에 성공하면 자동으로 해제됩니다.
                - 카드사 쪽에서 계정이 실제로 잠긴 경우(제한 임박 신호 수신)에는 더모아가 대신 풀어드릴 수 없습니다. 카드사 앱·홈페이지·고객센터에서 본인인증 후 잠금을 해제한 뒤 다시 연결해 주세요.

                카드사 로그인 자격증명은 최초 연결 시 1회만 암호화 전송되며 더모아 서버에 저장되지 않습니다.
                """, 90, now);
    }

    private Faq cardSyncBackfill(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "카드 연동 시 예전 결제 내역도 불러와지나요?", """
                네, 카드를 처음 연결하면 **최근 3개월치 결제 내역**을 백필(소급 수집)합니다.

                이 3개월은 고정지출 자동 추천 판정에도 쓰입니다. 같은 가맹점에서 3회 이상, 매달 비슷한 날짜(28~33일 주기)에, 비슷한 금액(±10% 이내)으로 결제된 내역이 있으면 고정지출 후보로 추천해 드려요.

                수기로 미리 입력해 둔 카드 결제 내역이 있다면, 백필된 내역과 자동으로 대조해 중복 없이 정본으로 대체됩니다.
                """, 80, now);
    }

    private Faq cardSyncMultipleIssuers(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "여러 카드사를 동시에 연결할 수 있나요?", """
                네, 지원하는 카드사 범위 안에서 여러 곳을 동시에 연결할 수 있습니다.

                한 카드사 계정에 카드가 여러 장(예: 체크카드+신용카드) 있다면 카드를 따로 선택하지 않고 **그 계정의 카드를 전부 자동으로 수집**합니다.
                """, 60, now);
    }

    private Faq dailyBudgetFormula(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "하루 권장 소비액은 어떤 기준으로 계산되나요?", """
                하루 권장 소비액은 아래 공식으로 계산되며, **당일 하루 동안은 고정된 값**을 유지합니다.

                > 하루 권장 소비액 = (월 예산 − 어제까지 이번 급여주기 누적 순지출) ÷ 오늘을 포함한 남은 일수

                여기서 **월 예산**은 `월급 − 고정지출 합계 − 월 저축 목표`입니다. 오늘 결제한 금액이나 취소·환불 내역은 **다음 날 하루 권장액**부터 반영되므로, 오늘 목표가 중간에 흔들리지 않습니다.
                """, 100, now);
    }

    private Faq dailyBudgetMidCycleChange(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "월급이나 저축 목표를 중간에 바꾸면 오늘 권장액이 바로 바뀌나요?", """
                하루 권장액은 **날짜가 바뀔 때마다** 다시 계산됩니다. 월급·저축 목표를 수정하면 그 시점 이후의 계산에는 즉시 반영되지만, 이미 확정되어 표시된 **오늘 하루의 권장액 자체는 중간에 바뀌지 않습니다.**

                다음 날 자정이 지나면 바뀐 값이 반영된 새 하루 권장액을 보여드립니다.
                """, 70, now);
    }

    private Faq dailyBudgetPaydayChange(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "월급일을 10일에서 25일로 바꿨는데 이번 달 예산에 바로 반영되나요?", """
                아니요. 급여일을 변경해도 **진행 중인 급여주기는 그대로 유지**되고, 변경한 급여일은 **다음 급여주기부터** 적용됩니다.

                이미 시작된 주기의 예산·하루 권장액을 소급해서 다시 계산하지 않으니, 갑자기 남은 예산이 크게 바뀌는 일은 없습니다.
                """, 65, now);
    }

    private Faq dailyBudgetSurplus(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "이번 달에 다 못 쓴 예산(잉여금)은 다음 달로 이월되나요?", """
                아니요, 잉여금은 **다음 달로 이월되지 않습니다.** 급여주기가 끝나면 그 주기의 예산은 마감되고, 새 주기는 다시 월급 기준으로 새로 계산됩니다.

                대신 남은 잉여금은 저축·투자에 도움이 될 수 있는 **금융상품 추천**에 활용됩니다. [상품 추천] 메뉴에서 확인해 보세요.
                """, 50, now);
    }

    private Faq fixedExpenseDoubleCount(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "고정지출로 등록해 둔 구독 결제가 하루 지출에서 두 번 빠지는 것 같아요.", """
                더모아는 고정지출이 월 예산에서 미리 차감되어 하루 권장액에 이중 계산되는 것을 방지하는 **이중 차감 방지·매칭** 로직을 제공합니다.

                등록된 고정지출(예: 넷플릭스 17,000원)과 실제 카드 결제 내역이 매칭되면 당일 소비 지출 합계에서 자동으로 제외됩니다. 매칭되지 않았다면 가맹점 표기가 달라 일시적으로 미매칭된 것일 수 있으니, [고정지출] 메뉴의 미납 알림에서 해당 결제건을 선택해 정정해 주세요.
                """, 100, now);
    }

    private Faq fixedExpenseAutoDetect(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "고정지출은 어떻게 자동으로 추천되나요?", """
                같은 가맹점(또는 서비스)에서 아래 조건을 모두 만족하면 고정지출 후보로 자동 추천됩니다.

                - 최근 3개월 내 **3회 이상** 결제
                - 결제 주기가 매달 **28~33일 간격**
                - 결제 금액이 **동일하거나 ±10% 이내**로 유사

                카드 연동 직후에는 최근 3개월치만 백필되어 있어 판단할 내역이 부족할 수 있습니다. 조금 더 이용하시면 추천이 나타납니다.
                """, 80, now);
    }

    private Faq fixedExpenseExchangeRate(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "해외 구독(Netflix, Claude 등) 결제의 환율 계산이 실제 청구 금액과 달라요.", """
                해외 결제는 결제 시점의 카드사 환율과 해외이용수수료가 더해져 최종 원화 청구액에 약간의 차이가 발생할 수 있습니다. 국내 결제는 원화 금액이 등록 금액의 **±10% 이내**면 같은 결제로 인식합니다.

                차이가 계속 크게 발생한다면 고정지출 상세에서 실제 청구된 원화 금액으로 직접 보정할 수 있습니다.
                """, 60, now);
    }

    private Faq fixedExpenseAmountChange(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "구독료가 올랐는데 별도로 안내가 없어요.", """
                등록된 고정지출과 매칭되는 결제 금액이 허용 오차(±10%)를 벗어나면 **결제 금액 변경 감지** 알림을 보내드립니다. 이용 중인 구독 요금이 실제로 올랐다면 [고정지출] 메뉴에서 등록된 금액을 새 금액으로 업데이트해 주세요.

                업데이트하지 않아도 결제는 정상적으로 반영되지만, 예산 계산에 쓰이는 예상 금액은 등록값 그대로입니다.
                """, 55, now);
    }

    private Faq manualExpenseCashTransfer(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "현금이나 계좌이체 지출도 수기로 작성하면 카드 내역과 중복되나요?", """
                아니요. 현금·계좌이체 지출은 카드사 수집 대상이 아니므로 **수기 입력이 반드시 필요**합니다.

                카드 자동수집이 켜져 있는 동안에는 중복 방지를 위해 결제수단이 '카드'인 지출의 수기 입력만 제한됩니다. 현금·계좌이체는 언제든 자유롭게 입력할 수 있습니다.
                """, 100, now);
    }

    private Faq manualExpenseCardSyncLater(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "수기로 입력해 둔 카드 결제를 나중에 카드 연동하면 어떻게 되나요?", """
                카드를 연동하면 과거 수기로 입력해 둔 카드 결제 중 실제 카드사 내역과 날짜·금액이 일치하는 건은 자동으로 **정본(카드 수집 내역)으로 대체**됩니다. 데이터가 두 배로 집계되지 않습니다.

                짝을 찾지 못한 수기 카드 건이 남아 있다면, 아직 연결하지 않은 다른 카드가 있을 수 있다는 **미연동 카드 의심 알림**을 보내드립니다.
                """, 70, now);
    }

    private Faq manualExpenseWhileSyncing(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "카드 자동수집 중에도 다시 수기 입력 모드로 돌아갈 수 있나요?", """
                네, [설정]에서 카드 자동수집을 끌 수 있습니다. 다만 이는 자동수집 여부를 끄고 켜는 것일 뿐, 한 번 카드 연동으로 전환한 입력 모드 자체가 되돌아가지는 않습니다. 자동수집을 끄면 그 시점부터 결제수단이 카드인 지출도 다시 수기로 입력할 수 있습니다.

                기존에 자동 수집된 내역은 그대로 보존됩니다.
                """, 50, now);
    }

    private Faq accountPasswordChange(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "비밀번호를 바꾸거나 회원 탈퇴를 하면 기존 정보는 어떻게 되나요?", """
                **비밀번호 변경 시:** 계정 보안을 위해 비밀번호 변경 즉시 접속 중이던 **모든 기기에서 강제 로그아웃**됩니다. 변경한 새 비밀번호로 다시 로그인해 주세요.

                **회원 탈퇴 시:** 등록된 카드사 연결 정보가 즉시 파기되며, 새벽 자동 수집 대상에서 제외됩니다.
                """, 100, now);
    }

    private Faq accountLoginLock(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "로그인 비밀번호를 여러 번 틀렸더니 계정이 잠겼어요.", """
                더모아 로그인 비밀번호를 **5회 연속 잘못 입력**하면 계정이 **15분간 잠깁니다.** 잠금 시간이 지나면 자동으로 해제되어 다시 로그인할 수 있습니다.

                이 잠금은 더모아 로그인 전용이며, 카드사 계정 잠금과는 별개입니다.
                """, 70, now);
    }

    private Faq accountMultiDeviceLogout(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "여러 기기에서 로그인했는데 지금 쓰는 기기만 로그아웃하고 싶어요.", """
                일반 로그아웃은 **현재 사용 중인 기기의 세션만** 종료합니다. 다른 기기의 로그인 상태는 그대로 유지됩니다.

                분실·도용이 의심되어 **모든 기기를 한 번에 로그아웃**하고 싶다면 비밀번호를 변경해 주세요. 비밀번호 변경 시 모든 기기의 로그인이 즉시 무효화됩니다.
                """, 40, now);
    }

    private Faq policyRecommendBasis(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "정책·금융상품 추천은 어떤 기준으로 나오나요?", """
                청년 정책은 나이·지역 등 회원님의 프로필 조건에 맞는 정책을 찾아 보여드립니다. 금융상품(예금·적금 등)은 급여주기가 끝난 뒤 남은 잉여금을 기준으로 도움이 될 수 있는 상품을 추천합니다.

                추천은 참고용 정보이며 실제 가입 여부는 각 상품·정책의 공식 조건을 다시 확인한 뒤 결정해 주세요.
                """, 60, now);
    }

    private Faq policyRegionLater(FaqCategory category, LocalDateTime now) {
        return Faq.seed(category, "가입할 때 입력하지 않은 지역 정보를 나중에 입력해야 하나요?", """
                네. 지역 정보는 가입 시 필수 입력이 아니며, **정책 추천 화면에 처음 들어갈 때** 입력받습니다. 지역 기반 청년 정책을 정확히 추천해 드리기 위해서만 사용됩니다.
                """, 30, now);
    }
}
