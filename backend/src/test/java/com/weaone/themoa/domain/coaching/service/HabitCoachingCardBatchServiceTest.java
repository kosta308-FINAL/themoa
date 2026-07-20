package com.weaone.themoa.domain.coaching.service;

import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.coaching.entity.CoachingCard;
import com.weaone.themoa.domain.coaching.entity.CoachingCardTargetType;
import com.weaone.themoa.domain.coaching.repository.CoachingCardRepository;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.IncomeType;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/** 습관 코칭 카드 생성 오케스트레이션(habitExpense.md §3·§4) 검증. */
@ExtendWith(MockitoExtension.class)
class HabitCoachingCardBatchServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private MerchantAliasRepository merchantAliasRepository;
    @Mock
    private CoachingCardRepository coachingCardRepository;
    @Mock
    private HabitCoachingCandidateExtractionService candidateExtractionService;
    @Mock
    private HabitCoachingLlmClient habitCoachingLlmClient;
    @Mock
    private HabitCoachingTemplateCardFactory templateCardFactory;

    private HabitCoachingCardBatchService service() {
        return new HabitCoachingCardBatchService(memberRepository, categoryRepository, merchantAliasRepository,
                coachingCardRepository, candidateExtractionService, habitCoachingLlmClient, templateCardFactory);
    }

    private Member member(int payday) {
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1), LocalDateTime.now());
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        member.configureSpendingGuide(IncomeType.SALARY, BigDecimal.valueOf(3_000_000), null, payday);
        return member;
    }

    private HabitCoachingCandidate categoryCandidate() {
        return new HabitCoachingCandidate(CoachingCardTargetType.CATEGORY, 10L, null, "외식", 10,
                BigDecimal.valueOf(475_000), BigDecimal.valueOf(47_500), BigDecimal.valueOf(475_000),
                BigDecimal.valueOf(142_500), false);
    }

    @Test
    @DisplayName("이미 그 주기 카드가 있으면 후보 추출도, 저장도 하지 않는다(멱등)")
    void skipsWhenAlreadyGeneratedForYearMonth() {
        Member member = member(25);
        given(memberRepository.getReferenceById(MEMBER_ID)).willReturn(member);
        given(coachingCardRepository.existsByMember_IdAndYearMonth(eq(MEMBER_ID), any())).willReturn(true);

        service().generateForMember(MEMBER_ID, LocalDate.of(2026, 7, 25));

        then(candidateExtractionService).should(never()).extractTopCandidates(any(), any(), any());
        then(coachingCardRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("후보가 없으면 카드를 저장하지 않는다(정상 빈 상태)")
    void savesNothingWhenNoCandidates() {
        Member member = member(25);
        given(memberRepository.getReferenceById(MEMBER_ID)).willReturn(member);
        given(coachingCardRepository.existsByMember_IdAndYearMonth(eq(MEMBER_ID), any())).willReturn(false);
        given(candidateExtractionService.extractTopCandidates(eq(member), any(), any())).willReturn(List.of());

        service().generateForMember(MEMBER_ID, LocalDate.of(2026, 7, 25));

        then(coachingCardRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("LLM 카드 문구가 기대 숫자를 포함하면 그대로 저장한다")
    void savesLlmDraftWhenNumericIntegrityHolds() {
        Member member = member(25);
        HabitCoachingCandidate candidate = categoryCandidate();
        Category category = Category.seed(CategoryCode.FOOD, "외식");
        ReflectionTestUtils.setField(category, "id", 10L);

        given(memberRepository.getReferenceById(MEMBER_ID)).willReturn(member);
        given(coachingCardRepository.existsByMember_IdAndYearMonth(eq(MEMBER_ID), any())).willReturn(false);
        given(candidateExtractionService.extractTopCandidates(eq(member), any(), any())).willReturn(List.of(candidate));
        given(habitCoachingLlmClient.generateDrafts(List.of(candidate))).willReturn(List.of(
                new CoachingCardDraft(candidate.targetRef(), "외식비가 눈에 띄어요",
                        "이번 주기 외식에 월 평균 475000원을 쓰고 있어요.")));
        given(categoryRepository.getReferenceById(10L)).willReturn(category);

        service().generateForMember(MEMBER_ID, LocalDate.of(2026, 7, 25));

        ArgumentCaptor<CoachingCard> captor = ArgumentCaptor.forClass(CoachingCard.class);
        then(coachingCardRepository).should().save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("외식비가 눈에 띄어요");
        then(templateCardFactory).should(never()).create(any());
    }

    @Test
    @DisplayName("LLM 카드 문구에 기대 숫자가 없으면 그 카드만 템플릿으로 교체한다")
    void fallsBackToTemplateWhenNumericIntegrityFails() {
        Member member = member(25);
        HabitCoachingCandidate candidate = categoryCandidate();
        Category category = Category.seed(CategoryCode.FOOD, "외식");
        ReflectionTestUtils.setField(category, "id", 10L);
        CoachingCardDraft templateDraft = new CoachingCardDraft(candidate.targetRef(), "템플릿 제목", "템플릿 본문 142500원");

        given(memberRepository.getReferenceById(MEMBER_ID)).willReturn(member);
        given(coachingCardRepository.existsByMember_IdAndYearMonth(eq(MEMBER_ID), any())).willReturn(false);
        given(candidateExtractionService.extractTopCandidates(eq(member), any(), any())).willReturn(List.of(candidate));
        given(habitCoachingLlmClient.generateDrafts(List.of(candidate))).willReturn(List.of(
                new CoachingCardDraft(candidate.targetRef(), "엉뚱한 제목", "숫자가 하나도 없는 본문입니다.")));
        given(templateCardFactory.create(candidate)).willReturn(templateDraft);
        given(categoryRepository.getReferenceById(10L)).willReturn(category);

        service().generateForMember(MEMBER_ID, LocalDate.of(2026, 7, 25));

        ArgumentCaptor<CoachingCard> captor = ArgumentCaptor.forClass(CoachingCard.class);
        then(coachingCardRepository).should().save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("템플릿 제목");
    }

    @Test
    @DisplayName("LLM 호출이 통째로 실패(빈 리스트)하면 전량 템플릿으로 저장한다")
    void fallsBackToTemplateWhenLlmReturnsEmpty() {
        Member member = member(25);
        HabitCoachingCandidate candidate = categoryCandidate();
        Category category = Category.seed(CategoryCode.FOOD, "외식");
        ReflectionTestUtils.setField(category, "id", 10L);
        CoachingCardDraft templateDraft = new CoachingCardDraft(candidate.targetRef(), "템플릿 제목", "템플릿 본문 142500원");

        given(memberRepository.getReferenceById(MEMBER_ID)).willReturn(member);
        given(coachingCardRepository.existsByMember_IdAndYearMonth(eq(MEMBER_ID), any())).willReturn(false);
        given(candidateExtractionService.extractTopCandidates(eq(member), any(), any())).willReturn(List.of(candidate));
        given(habitCoachingLlmClient.generateDrafts(List.of(candidate))).willReturn(List.of());
        given(templateCardFactory.create(candidate)).willReturn(templateDraft);
        given(categoryRepository.getReferenceById(10L)).willReturn(category);

        service().generateForMember(MEMBER_ID, LocalDate.of(2026, 7, 25));

        then(coachingCardRepository).should().save(any());
        then(templateCardFactory).should().create(candidate);
    }
}
