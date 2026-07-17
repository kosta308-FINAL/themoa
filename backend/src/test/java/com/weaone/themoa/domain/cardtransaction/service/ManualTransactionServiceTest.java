package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.cardtransaction.dto.request.ManualTransactionCreateRequest;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse;
import com.weaone.themoa.domain.cardtransaction.entity.PaymentMethod;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ManualTransactionServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long CATEGORY_ID = 3L;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CardTransactionRepository cardTransactionRepository;

    @InjectMocks
    private ManualTransactionService manualTransactionService;

    private Member member() {
        return Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1));
    }

    private ManualTransactionCreateRequest request(PaymentMethod paymentMethod) {
        return new ManualTransactionCreateRequest(paymentMethod, LocalDate.of(2026, 7, 10), null,
                BigDecimal.valueOf(9000), CATEGORY_ID, "편의점", "메모");
    }

    @Test
    @DisplayName("현금 수기 입력은 항상 허용된다")
    void createsCashEntry() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
        given(categoryRepository.findById(CATEGORY_ID))
                .willReturn(Optional.of(Category.seed(CategoryCode.CONVENIENCE, "편의점")));

        CardTransactionResponse response = manualTransactionService.create(MEMBER_ID, request(PaymentMethod.CASH));

        assertThat(response.amount()).isEqualByComparingTo("9000");
        then(cardTransactionRepository).should().save(any());
    }

    @Test
    @DisplayName("수기 모드 회원의 카드 결제수단 수기 입력은 허용된다")
    void allowsCardEntryInManualMode() {
        Member member = member();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(categoryRepository.findById(CATEGORY_ID))
                .willReturn(Optional.of(Category.seed(CategoryCode.SHOPPING, "쇼핑")));

        manualTransactionService.create(MEMBER_ID, request(PaymentMethod.CARD));

        then(cardTransactionRepository).should().save(any());
    }

    @Test
    @DisplayName("카드 모드 + 자동수집 ON 상태에서 카드 결제수단 수기 입력은 403으로 거부한다")
    void rejectsCardEntryWhenSyncRunning() {
        Member member = member();
        member.startCardSync(LocalDateTime.now());
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> manualTransactionService.create(MEMBER_ID, request(PaymentMethod.CARD)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MANUAL_CARD_ENTRY_NOT_ALLOWED);
        then(cardTransactionRepository).should(never()).save(any());
        then(categoryRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("자동수집을 꺼두면 카드 모드에서도 카드 결제수단 수기 입력이 다시 허용된다")
    void allowsCardEntryWhenSyncDisabled() {
        Member member = member();
        member.startCardSync(LocalDateTime.now());
        member.disableCardSync();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(categoryRepository.findById(CATEGORY_ID))
                .willReturn(Optional.of(Category.seed(CategoryCode.SHOPPING, "쇼핑")));

        manualTransactionService.create(MEMBER_ID, request(PaymentMethod.CARD));

        then(cardTransactionRepository).should().save(any());
    }

    @Test
    @DisplayName("존재하지 않는 카테고리는 404로 거부한다")
    void rejectsUnknownCategory() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
        given(categoryRepository.findById(CATEGORY_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> manualTransactionService.create(MEMBER_ID, request(PaymentMethod.CASH)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }
}
