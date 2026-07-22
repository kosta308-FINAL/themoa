package com.weaone.themoa.domain.member.support;

import com.weaone.themoa.config.AuthProperties;
import com.weaone.themoa.domain.auth.entity.MemberTermsAgreement;
import com.weaone.themoa.domain.auth.entity.TermsType;
import com.weaone.themoa.domain.auth.repository.MemberTermsAgreementRepository;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.IncomeType;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.entity.Role;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 로컬 개발용 데모 회원 시드. 앱을 처음 띄워도 로그인해서 바로 둘러볼 수 있도록 소비 가이드까지 설정된
 * 프로필을 몇 개 채워 둔다. 운영 배포에는 이 러너를 쓰지 않는다(운영 DB는 실제 가입으로만 채운다).
 *
 * <p>모든 데모 계정의 비밀번호 해시는 {@value #DEMO_PASSWORD_HASH}(평문 "1111")다.
 */
@Component
@Order(4)
@RequiredArgsConstructor
public class MemberDemoSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD_HASH =
            "$2a$10$gFDQQs84GQbyA12PQnkecOTti2Xy6hErVG271TQq7xHgL8aWy8B4y";
    public static final String ADMIN_EMAIL = "admin";
    public static final String SOLMIN_EMAIL = "solmin";

    private final MemberRepository memberRepository;
    private final MemberTermsAgreementRepository memberTermsAgreementRepository;
    private final AuthProperties authProperties;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.count() > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE member AUTO_INCREMENT = 1");
        LocalDateTime now = LocalDateTime.now();
        String hash = DEMO_PASSWORD_HASH;

        Member admin = Member.signUp(ADMIN_EMAIL, hash, "운영자", Gender.FEMALE, LocalDate.of(1993, 3, 2), now);
        admin.changeRole(Role.ADMIN);
        admin.recordLoginSuccess(now);
        memberRepository.save(admin);

        Member solmin = Member.signUp(SOLMIN_EMAIL, hash, "김솔민", Gender.FEMALE, LocalDate.of(1999, 11, 14), now);
        solmin.configureSpendingGuide(IncomeType.SALARY, BigDecimal.valueOf(3_000_000), null, 5);
        solmin.changeSavingsTarget(BigDecimal.valueOf(500_000));
        solmin.recordLoginSuccess(now);
        memberRepository.save(solmin);

        Member jaehoon = Member.signUp("test1", hash, "정재훈", Gender.MALE, LocalDate.of(1997, 6, 21), now);
        jaehoon.configureSpendingGuide(IncomeType.SALARY, BigDecimal.valueOf(2_400_000), null, 25);
        jaehoon.changeSavingsTarget(BigDecimal.valueOf(300_000));
        jaehoon.recordLoginSuccess(now);
        memberRepository.save(jaehoon);

        Member areum = Member.signUp("test2", hash, "박아름", Gender.FEMALE, LocalDate.of(2001, 2, 9), now);
        areum.configureSpendingGuide(IncomeType.HOURLY, null, BigDecimal.valueOf(10_030), 10);
        areum.recordLoginSuccess(now);
        memberRepository.save(areum);

        Member newbie = Member.signUp("test3", hash, "이신규", Gender.MALE, LocalDate.of(2000, 8, 30), now);
        memberRepository.save(newbie);

        // 관리자 "가맹점 & 서비스 마스터" 화면(AdminMerchantService)이 DATA_COLLECTION 동의 회원만
        // 집계하므로, 그 화면의 데모 시나리오(MerchantAliasPromotionDemoSeeder)에 등장하는 회원들은
        // 여기서 미리 동의 이력을 심어 둔다 — 실제 가입은 체크박스로 선택 동의하지만(AuthService), 데모
        // 회원은 가입 폼을 거치지 않아 이 행이 없으면 화면이 통째로 빈 상태가 된다.
        agreeToDataCollection(now, solmin, jaehoon, areum, newbie);
    }

    private void agreeToDataCollection(LocalDateTime now, Member... members) {
        String version = authProperties.terms().version();
        for (Member member : members) {
            memberTermsAgreementRepository.save(
                    MemberTermsAgreement.agree(member, TermsType.DATA_COLLECTION, version, now));
        }
    }
}
