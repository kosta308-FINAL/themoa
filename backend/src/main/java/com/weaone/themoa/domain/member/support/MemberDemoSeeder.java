package com.weaone.themoa.domain.member.support;

import com.weaone.themoa.domain.member.entity.Gender;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 시연용 계정 시드. member_id=1은 시연용 계정(email="test")으로 고정한다. 운영 배포에는 이 러너를 쓰지
 * 않는다(운영 DB는 실제 가입으로만 채운다).
 *
 * <p>두 계정 모두 비밀번호 해시는 {@value #DEMO_PASSWORD_HASH}다.
 */
@Component
@Order(4)
@RequiredArgsConstructor
public class MemberDemoSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD_HASH =
            "$2a$10$gFDQQs84GQbyA12PQnkecOTti2Xy6hErVG271TQq7xHgL8aWy8B4y";
    public static final String DEMO_EMAIL = "test";
    public static final String ADMIN_EMAIL = "admin";

    private final MemberRepository memberRepository;
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

        // member_id=1 고정 — 이 시더에서 가장 먼저 저장되는 계정이 id=1을 받는다.
        Member demo = Member.signUp(DEMO_EMAIL, hash, "더모아", Gender.FEMALE, LocalDate.of(1999, 1, 1), now);
        demo.recordLoginSuccess(now);
        memberRepository.save(demo);

        Member admin = Member.signUp(ADMIN_EMAIL, hash, "운영자", Gender.FEMALE, LocalDate.of(1993, 3, 2), now);
        admin.changeRole(Role.ADMIN);
        admin.recordLoginSuccess(now);
        memberRepository.save(admin);
    }
}
