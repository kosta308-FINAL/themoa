package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCoachingCard;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseCoachingCardRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 고정지출 코칭 카드 생성 오케스트레이션. 규칙 계층(dismiss·기부회비 제외) → LLM(대상 선정+문구) → 저장
 * 순서로 조립한다. 주기(yearMonth) 단위 존재 확인으로 멱등이다 — 조회 시점에 그 주기 카드가 없으면 그때
 * 만든다(습관 코칭과 달리 몇 개월치 데이터 축적을 기다릴 필요가 없어 새벽 배치 없이 지연 생성으로 충분하다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixedExpenseCoachingCardService {

    private final MemberRepository memberRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final FixedExpenseCoachingCardRepository coachingCardRepository;
    private final FixedExpenseCoachingCandidateExtractionService candidateExtractionService;
    private final FixedExpenseCoachingLlmClient coachingLlmClient;
    private final FixedExpenseCoachingTemplateCardFactory templateCardFactory;
    private final BudgetCycleService budgetCycleService;

    /** 이번 주기 카드가 이미 있으면 아무 것도 하지 않는다(멱등). */
    @Transactional
    public void generateForMemberIfMissing(Long memberId, String yearMonth) {
        if (coachingCardRepository.existsByMember_IdAndYearMonth(memberId, yearMonth)) {
            return;
        }
        Member member = memberRepository.getReferenceById(memberId);
        List<FixedExpenseCoachingCandidate> candidates = candidateExtractionService.extractCandidates(member);
        if (candidates.isEmpty()) {
            return; // 정상: 후보가 없으면 카드도 없다.
        }

        List<FixedExpenseCoachingDraft> drafts = coachingLlmClient.selectAndDraft(candidates);
        if (drafts.isEmpty()) {
            return; // 정상: LLM이 전부 필수 지출로 판단했거나 호출 자체가 실패했다.
        }

        Map<String, FixedExpenseCoachingCandidate> candidatesByRef = candidates.stream()
                .collect(Collectors.toMap(FixedExpenseCoachingCandidate::targetRef, candidate -> candidate));

        LocalDateTime now = LocalDateTime.now(FixedExpenseCyclePolicy.ZONE_SEOUL);
        short displayOrder = 1;
        for (FixedExpenseCoachingDraft draft : drafts) {
            FixedExpenseCoachingCandidate candidate = candidatesByRef.get(draft.targetRef());
            if (candidate == null) {
                log.warn("고정지출 코칭 LLM이 알 수 없는 targetRef를 반환해 건너뜁니다. targetRef={}", draft.targetRef());
                continue; // LLM이 후보에 없는 targetRef를 지어냄 — 신뢰하지 않는다.
            }
            FixedExpenseCoachingDraft resolved = resolveDraft(candidate, draft);
            saveCard(member, yearMonth, candidate, resolved, displayOrder, now);
            displayOrder++;
        }
    }

    /** LLM이 이미 대상으로 고른 후보라도 문구가 숫자 무결성 검사를 통과하지 못하면 템플릿으로 교체한다. */
    private FixedExpenseCoachingDraft resolveDraft(FixedExpenseCoachingCandidate candidate,
                                                     FixedExpenseCoachingDraft draft) {
        if (!hasNumericIntegrity(candidate, draft)) {
            return templateCardFactory.create(candidate);
        }
        return draft;
    }

    private boolean hasNumericIntegrity(FixedExpenseCoachingCandidate candidate, FixedExpenseCoachingDraft draft) {
        if (draft.title() == null || draft.title().isBlank() || draft.body() == null || draft.body().isBlank()) {
            return false;
        }
        String expectedAnnual = candidate.annualAmount().toBigInteger().toString();
        return draft.body().contains(expectedAnnual);
    }

    private void saveCard(Member member, String yearMonth, FixedExpenseCoachingCandidate candidate,
                           FixedExpenseCoachingDraft draft, short displayOrder, LocalDateTime now) {
        FixedExpense fixedExpense = fixedExpenseRepository.getReferenceById(candidate.fixedExpenseId());
        FixedExpenseCoachingCard card = FixedExpenseCoachingCard.of(member, yearMonth, fixedExpense, draft.title(),
                draft.body(), candidate.annualAmount(), displayOrder, now);
        try {
            coachingCardRepository.save(card);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 경합 — 이미 다른 요청이 같은 주기 카드를 저장했다. 무시한다.
        }
    }
}
