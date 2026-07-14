package com.weaone.themoa.domain.merchant.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.entity.MerchantAliasTerms;
import com.weaone.themoa.domain.merchant.repository.BillerRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasTermsRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MerchantIdentityServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final String RAW_NAME = "ANTHROPIC* CLAUDE SUB";

    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private MerchantAliasRepository merchantAliasRepository;
    @Mock
    private MerchantAliasTermsRepository merchantAliasTermsRepository;
    @Mock
    private BillerRepository billerRepository;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MerchantIdentityService merchantIdentityService;

    private MerchantAlias aliasWithId(long id, String name) {
        MerchantAlias alias = MerchantAlias.create(name, 7L);
        ReflectionTestUtils.setField(alias, "id", id);
        return alias;
    }

    private Merchant merchantWithId(long id, String rawName, MerchantAlias globalAlias) {
        Merchant merchant = Merchant.observe(rawName, globalAlias);
        ReflectionTestUtils.setField(merchant, "id", id);
        return merchant;
    }

    @Test
    @DisplayName("내 학습 term과 전역 시드 term이 둘 다 걸리면 내 term이 이긴다")
    void resolvePrefersMyTermOverGlobal() {
        Merchant merchant = merchantWithId(10L, RAW_NAME, null);
        MerchantAlias myAlias = aliasWithId(1L, "내가 등록한 서비스");
        MerchantAliasTerms myTerm = MerchantAliasTerms.learn(myAlias, null, RAW_NAME);
        given(merchantRepository.findByMerchantNameRaw(RAW_NAME)).willReturn(Optional.of(merchant));
        given(billerRepository.existsByNameNormalized(RAW_NAME)).willReturn(false);
        given(merchantAliasTermsRepository.findMineByRawName(MEMBER_ID, RAW_NAME)).willReturn(Optional.of(myTerm));

        MerchantIdentityResult result = merchantIdentityService.resolve(MEMBER_ID, RAW_NAME);

        assertThat(result.biller()).isFalse();
        assertThat(result.merchantAliasId()).isEqualTo(1L);
        then(merchantAliasTermsRepository).should(never()).findGlobalByRawName(any());
    }

    @Test
    @DisplayName("내 term이 없으면 전역 시드 term으로 판별한다")
    void resolveFallsBackToGlobalTerm() {
        Merchant merchant = merchantWithId(10L, RAW_NAME, null);
        MerchantAlias claude = aliasWithId(2L, "Claude 구독");
        MerchantAliasTerms globalTerm = MerchantAliasTerms.seed(claude, RAW_NAME);
        given(merchantRepository.findByMerchantNameRaw(RAW_NAME)).willReturn(Optional.of(merchant));
        given(billerRepository.existsByNameNormalized(RAW_NAME)).willReturn(false);
        given(merchantAliasTermsRepository.findMineByRawName(MEMBER_ID, RAW_NAME)).willReturn(Optional.empty());
        given(merchantAliasTermsRepository.findGlobalByRawName(RAW_NAME)).willReturn(Optional.of(globalTerm));

        MerchantIdentityResult result = merchantIdentityService.resolve(MEMBER_ID, RAW_NAME);

        assertThat(result.merchantAliasId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("term 매칭이 전부 실패하면 merchant에 이미 연결된 전역 alias로 판별한다")
    void resolveFallsBackToMerchantGlobalAlias() {
        MerchantAlias claude = aliasWithId(3L, "Claude 구독");
        Merchant merchant = merchantWithId(10L, RAW_NAME, claude);
        given(merchantRepository.findByMerchantNameRaw(RAW_NAME)).willReturn(Optional.of(merchant));
        given(billerRepository.existsByNameNormalized(RAW_NAME)).willReturn(false);
        given(merchantAliasTermsRepository.findMineByRawName(MEMBER_ID, RAW_NAME)).willReturn(Optional.empty());
        given(merchantAliasTermsRepository.findGlobalByRawName(RAW_NAME)).willReturn(Optional.empty());

        MerchantIdentityResult result = merchantIdentityService.resolve(MEMBER_ID, RAW_NAME);

        assertThat(result.merchantAliasId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("아무것도 못 찾으면 추측하지 않고 alias를 NULL로 끝낸다")
    void resolveReturnsNullAliasWhenNothingMatches() {
        Merchant merchant = merchantWithId(11L, "복뚱이네 간장게장", null);
        given(merchantRepository.findByMerchantNameRaw("복뚱이네 간장게장")).willReturn(Optional.of(merchant));
        given(billerRepository.existsByNameNormalized("복뚱이네 간장게장")).willReturn(false);
        given(merchantAliasTermsRepository.findMineByRawName(MEMBER_ID, "복뚱이네 간장게장")).willReturn(Optional.empty());
        given(merchantAliasTermsRepository.findGlobalByRawName("복뚱이네 간장게장")).willReturn(Optional.empty());

        MerchantIdentityResult result = merchantIdentityService.resolve(MEMBER_ID, "복뚱이네 간장게장");

        assertThat(result.merchantAliasId()).isNull();
        assertThat(result.biller()).isFalse();
    }

    @Test
    @DisplayName("biller면 이름 term 검색을 건너뛰고 biller 결과만 반환한다")
    void resolveSkipsTermLookupForBiller() {
        Merchant merchant = merchantWithId(12L, "Apple", null);
        given(merchantRepository.findByMerchantNameRaw("Apple")).willReturn(Optional.of(merchant));
        given(billerRepository.existsByNameNormalized("Apple")).willReturn(true);

        MerchantIdentityResult result = merchantIdentityService.resolve(MEMBER_ID, "Apple");

        assertThat(result.biller()).isTrue();
        assertThat(result.merchantAliasId()).isNull();
        then(merchantAliasTermsRepository).should(never()).findMineByRawName(any(), any());
        then(merchantAliasTermsRepository).should(never()).findGlobalByRawName(any());
    }

    @Test
    @DisplayName("처음 보는 원본명이면 merchant를 새로 만들고, 전역 term이 있으면 즉시 연결한다")
    void resolveCreatesMerchantWithGlobalAliasLink() {
        MerchantAlias claude = aliasWithId(4L, "Claude 구독");
        MerchantAliasTerms globalTerm = MerchantAliasTerms.seed(claude, RAW_NAME);
        given(merchantRepository.findByMerchantNameRaw(RAW_NAME)).willReturn(Optional.empty());
        given(merchantAliasTermsRepository.findGlobalByRawName(RAW_NAME)).willReturn(Optional.of(globalTerm));
        given(merchantRepository.save(any(Merchant.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(billerRepository.existsByNameNormalized(RAW_NAME)).willReturn(false);
        given(merchantAliasTermsRepository.findMineByRawName(MEMBER_ID, RAW_NAME)).willReturn(Optional.empty());

        MerchantIdentityResult result = merchantIdentityService.resolve(MEMBER_ID, RAW_NAME);

        assertThat(result.merchantAliasId()).isEqualTo(4L);
        then(merchantRepository).should().save(any(Merchant.class));
    }

    @Test
    @DisplayName("같은 서비스명이 이미 있으면 재사용하고 새로 만들지 않는다")
    void registerAliasReusesExisting() {
        MerchantAlias existing = aliasWithId(5L, "Claude 구독");
        given(merchantAliasRepository.findByCanonicalServiceNameNormalized(" claude 구독 ")).willReturn(Optional.of(existing));

        MerchantAlias result = merchantIdentityService.registerAlias(MEMBER_ID, " claude 구독 ", 7L, null);

        assertThat(result).isEqualTo(existing);
        then(merchantAliasRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("새 서비스명이면 alias를 새로 만든다")
    void registerAliasCreatesNew() {
        given(merchantAliasRepository.findByCanonicalServiceNameNormalized("웨이브")).willReturn(Optional.empty());
        given(merchantAliasRepository.save(any(MerchantAlias.class))).willAnswer(invocation -> invocation.getArgument(0));

        MerchantAlias result = merchantIdentityService.registerAlias(MEMBER_ID, "웨이브", 7L, null);

        assertThat(result.getCanonicalServiceName()).isEqualTo("웨이브");
        then(merchantAliasRepository).should().save(any(MerchantAlias.class));
    }

    @Test
    @DisplayName("결제대행사 이름은 표기로 학습시킬 수 없다")
    void learnTermRejectsBillerName() {
        given(billerRepository.existsByNameNormalized("Apple")).willReturn(true);

        assertThatThrownBy(() -> merchantIdentityService.learnTerm(MEMBER_ID, 1L, "Apple"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MERCHANT_ALIAS_TERM_BILLER_FORBIDDEN);
        then(merchantAliasTermsRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("같은 표기가 이미 다른 alias를 가리키면 409로 거부한다")
    void learnTermRejectsConflict() {
        MerchantAlias targetAlias = aliasWithId(1L, "Claude 구독");
        MerchantAlias otherAlias = aliasWithId(2L, "다른 서비스");
        MerchantAliasTerms conflicting = MerchantAliasTerms.learn(otherAlias, null, RAW_NAME);
        given(billerRepository.existsByNameNormalized(RAW_NAME)).willReturn(false);
        given(merchantAliasRepository.findById(1L)).willReturn(Optional.of(targetAlias));
        given(merchantAliasTermsRepository.findByMember_IdAndAliasText(MEMBER_ID, RAW_NAME))
                .willReturn(Optional.of(conflicting));

        assertThatThrownBy(() -> merchantIdentityService.learnTerm(MEMBER_ID, 1L, RAW_NAME))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MERCHANT_ALIAS_TERM_CONFLICT);
        then(merchantAliasTermsRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("이미 같은 alias로 학습된 표기는 다시 저장하지 않는다(멱등)")
    void learnTermIsIdempotentForSameAlias() {
        MerchantAlias alias = aliasWithId(1L, "Claude 구독");
        MerchantAliasTerms already = MerchantAliasTerms.learn(alias, null, RAW_NAME);
        given(billerRepository.existsByNameNormalized(RAW_NAME)).willReturn(false);
        given(merchantAliasRepository.findById(1L)).willReturn(Optional.of(alias));
        given(merchantAliasTermsRepository.findByMember_IdAndAliasText(MEMBER_ID, RAW_NAME))
                .willReturn(Optional.of(already));

        merchantIdentityService.learnTerm(MEMBER_ID, 1L, RAW_NAME);

        then(merchantAliasTermsRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("새 표기를 그 사용자 소유로 저장한다")
    void learnTermSavesNewTerm() {
        MerchantAlias alias = aliasWithId(1L, "Claude 구독");
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1));
        given(billerRepository.existsByNameNormalized(RAW_NAME)).willReturn(false);
        given(merchantAliasRepository.findById(1L)).willReturn(Optional.of(alias));
        given(merchantAliasTermsRepository.findByMember_IdAndAliasText(MEMBER_ID, RAW_NAME)).willReturn(Optional.empty());
        given(memberRepository.getReferenceById(MEMBER_ID)).willReturn(member);

        merchantIdentityService.learnTerm(MEMBER_ID, 1L, RAW_NAME);

        then(merchantAliasTermsRepository).should().save(any(MerchantAliasTerms.class));
    }

    @Test
    @DisplayName("존재하지 않는 alias로 학습하려 하면 404로 거부한다")
    void learnTermRejectsUnknownAlias() {
        given(billerRepository.existsByNameNormalized(RAW_NAME)).willReturn(false);
        given(merchantAliasRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> merchantIdentityService.learnTerm(MEMBER_ID, 99L, RAW_NAME))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MERCHANT_ALIAS_NOT_FOUND);
    }
}
