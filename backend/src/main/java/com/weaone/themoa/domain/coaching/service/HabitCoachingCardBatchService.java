package com.weaone.themoa.domain.coaching.service;

import com.weaone.themoa.domain.budget.service.BudgetCyclePolicy;
import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.coaching.entity.CoachingCard;
import com.weaone.themoa.domain.coaching.entity.CoachingCardTargetType;
import com.weaone.themoa.domain.coaching.repository.CoachingCardRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 습관 코칭 카드 생성 오케스트레이션(habitExpense.md §3·§4·§8). 규칙 계층 → LLM(또는 폴백 템플릿) → 저장
 * 순서로 조립한다. 트리거는 두 곳: 월급일 새벽 배치(§3 "생성 주기 = 월급 주기 1회 배치")와 최초 3개월
 * 백필 완료 이벤트(§3 "최초 카드 3개월 백필 완료 시 1회 즉시 생성") — 같은 {@link #generateForMember} 코드를
 * 공유하고, {@code year_month} 단위 존재 확인으로 두 트리거가 겹쳐도 중복 생성되지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HabitCoachingCardBatchService {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final MerchantAliasRepository merchantAliasRepository;
    private final CoachingCardRepository coachingCardRepository;
    private final HabitCoachingCandidateExtractionService candidateExtractionService;
    private final HabitCoachingLlmClient habitCoachingLlmClient;
    private final HabitCoachingTemplateCardFactory templateCardFactory;
    private final BudgetCycleService budgetCycleService;

    /**
     * 회원별 급여일 새벽 배치. 오늘이 실제 급여일(말일 보정 포함)인 회원만 대상으로 한다. 매일 전 회원을
     * 훑는 유일한 배치라 급여일 변경 예약(pendingPayday) 승격도 여기서 겸해 처리한다 — 사용자가 앱을 안
     * 열어도 하루 안에는 승격이 보장된다(payday.md §급여일 변경). isPaydayToday 판정은 승격 전 스냅샷 값을
     * 쓰므로 변경 당일 트리거가 하루 어긋날 수 있지만, generateForMember는 멱등이라 다음 배치에서 만회된다.
     */
    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void runPaydayBatch() {
        LocalDate today = LocalDate.now(ZONE_SEOUL);
        for (Member member : memberRepository.findByPaydayIsNotNull()) {
            budgetCycleService.ensurePaydayPromoted(member.getId(), today);
            if (!isPaydayToday(member.getPayday(), today)) {
                continue;
            }
            try {
                generateForMember(member.getId(), today);
            } catch (RuntimeException e) {
                log.warn("습관 코칭 카드 생성 1건 실패, 다음 회원으로 계속 진행합니다. memberId={}", member.getId(), e);
            }
        }
    }

    private boolean isPaydayToday(int payday, LocalDate today) {
        int effectiveDay = Math.min(payday, today.lengthOfMonth());
        return today.getDayOfMonth() == effectiveDay;
    }

    /** 가장 최근에 완료된 급여 주기 소비로 카드를 생성한다. 이미 그 주기 카드가 있으면 아무 것도 하지 않는다(멱등). */
    @Transactional
    public void generateForMember(Long memberId, LocalDate today) {
        Member member = memberRepository.getReferenceById(memberId);
        if (member.getPayday() == null) {
            return;
        }
        BudgetCyclePolicy.BudgetCycle previousCycle = budgetCycleService.previousCompletedCycle(member, today);
        String yearMonth = previousCycle.yearMonth();
        if (coachingCardRepository.existsByMember_IdAndYearMonth(memberId, yearMonth)) {
            return;
        }

        List<HabitCoachingCandidate> candidates = candidateExtractionService
                .extractTopCandidates(member, previousCycle.cycleStartDate(), previousCycle.cycleEndDate());
        if (candidates.isEmpty()) {
            return; // 정상: 후보가 없으면 카드도 없다(§5 빈 상태, 200 빈 배열).
        }

        List<CoachingCardDraft> drafts = habitCoachingLlmClient.generateDrafts(candidates);
        Map<String, CoachingCardDraft> draftsByRef = drafts.stream()
                .collect(Collectors.toMap(CoachingCardDraft::targetRef, draft -> draft, (a, b) -> a));

        LocalDateTime now = LocalDateTime.now(ZONE_SEOUL);
        short displayOrder = 1;
        for (HabitCoachingCandidate candidate : candidates) {
            CoachingCardDraft draft = resolveDraft(candidate, draftsByRef.get(candidate.targetRef()));
            saveCard(member, yearMonth, candidate, draft, displayOrder, now);
            displayOrder++;
        }
    }

    /** LLM 문구가 없거나 숫자 무결성 검사(§4)를 통과하지 못하면 그 카드만 템플릿으로 교체한다(카드별 폴백). */
    private CoachingCardDraft resolveDraft(HabitCoachingCandidate candidate, CoachingCardDraft draft) {
        if (draft == null || !hasNumericIntegrity(candidate, draft)) {
            return templateCardFactory.create(candidate);
        }
        return draft;
    }

    private boolean hasNumericIntegrity(HabitCoachingCandidate candidate, CoachingCardDraft draft) {
        if (draft.title() == null || draft.title().isBlank() || draft.body() == null || draft.body().isBlank()) {
            return false;
        }
        String expectedSaving = candidate.estimatedSaving().toBigInteger().toString();
        String expectedMonthly = candidate.monthlyAverage().toBigInteger().toString();
        return draft.body().contains(expectedSaving) || draft.body().contains(expectedMonthly);
    }

    private void saveCard(Member member, String yearMonth, HabitCoachingCandidate candidate, CoachingCardDraft draft,
                           short displayOrder, LocalDateTime now) {
        CoachingCard card = candidate.targetType() == CoachingCardTargetType.CATEGORY
                ? CoachingCard.forCategory(member, yearMonth, draft.title(), draft.body(),
                        categoryRepository.getReferenceById(candidate.categoryId()), candidate.estimatedSaving(),
                        displayOrder, now)
                : CoachingCard.forMerchantAlias(member, yearMonth, draft.title(), draft.body(),
                        merchantAliasRepository.getReferenceById(candidate.merchantAliasId()),
                        candidate.estimatedSaving(), displayOrder, now);
        try {
            coachingCardRepository.save(card);
        } catch (DataIntegrityViolationException e) {
            // 동시 트리거 경합(월급일 배치 ↔ 백필 완료 이벤트) — 이미 다른 쪽이 같은 주기 카드를 저장했다. 무시한다.
        }
    }
}
