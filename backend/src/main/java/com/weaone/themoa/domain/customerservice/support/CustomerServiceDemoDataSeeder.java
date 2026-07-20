package com.weaone.themoa.domain.customerservice.support;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiry;
import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryAnswer;
import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryCategory;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryAnswerRepository;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryCategoryRepository;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.member.support.MemberDemoSeeder;
import com.weaone.themoa.domain.notification.entity.NotificationTypeCode;
import com.weaone.themoa.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 로컬 개발용 데모 1:1 문의·답변·알림 시드. {@link MemberDemoSeeder}·{@code CustomerServiceMasterSeeder}·
 * {@code NotificationTypeSeeder}가 먼저 실행되어야 하므로 더 늦은 순번으로 돈다.
 */
@Component
@Order(5)
@RequiredArgsConstructor
public class CustomerServiceDemoDataSeeder implements ApplicationRunner {

    private final CustomerInquiryRepository inquiryRepository;
    private final CustomerInquiryCategoryRepository inquiryCategoryRepository;
    private final CustomerInquiryAnswerRepository answerRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (inquiryRepository.count() > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE customer_inquiry AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE customer_inquiry_answer AUTO_INCREMENT = 1");
        Optional<Member> solmin = memberRepository.findByEmail(MemberDemoSeeder.SOLMIN_EMAIL);
        Optional<Member> admin = memberRepository.findByEmail(MemberDemoSeeder.ADMIN_EMAIL);
        if (solmin.isEmpty() || admin.isEmpty()) {
            return;
        }
        List<CustomerInquiryCategory> categories = inquiryCategoryRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc();
        CustomerInquiryCategory cardSyncCategory = findByName(categories, "카드 연동");
        CustomerInquiryCategory fixedExpenseCategory = findByName(categories, "고정지출");
        if (cardSyncCategory == null || fixedExpenseCategory == null) {
            return;
        }

        LocalDateTime answeredAt = LocalDateTime.now().minusDays(1).minusHours(2);
        CustomerInquiry answeredInquiry = inquiryRepository.save(CustomerInquiry.create(
                solmin.get(), cardSyncCategory,
                "현대카드 자동 수집이 30분 넘게 안 들어와요.",
                "현대카드를 새로 연결했는데 최근 결제한 2건이 계속 안 뜨고 새로고침을 눌러도 반응이 없습니다.",
                "2026-07-20", answeredAt));

        LocalDateTime answerCreatedAt = answeredAt.plusHours(2);
        answerRepository.save(CustomerInquiryAnswer.create(
                answeredInquiry, admin.get(),
                "안녕하세요, 회원님! 더모아 고객센터입니다.\n\n"
                        + "확인 결과 현대카드 측 가맹점 승인 시스템 일시 점검으로 인해 약 40분간 결제 데이터 연동 수집이 지연되었습니다.\n"
                        + "현재 정상 복구되었으며 [카드/소비내역] 메뉴에서 '새로고침'을 누르시면 정상 반영됩니다. 이용에 불편을 드려 죄송합니다.",
                answerCreatedAt));
        answeredInquiry.markAnswered(answerCreatedAt);
        notificationService.createIfAbsent(solmin.get(), NotificationTypeCode.INQUIRY_ANSWERED,
                "문의에 답변이 등록되었습니다.", null, answeredInquiry,
                "INQUIRY_ANSWERED:inquiry=" + answeredInquiry.getId());

        inquiryRepository.save(CustomerInquiry.create(
                solmin.get(), fixedExpenseCategory,
                "해외 구독(Claude) 환율 계산이 실제 청구액과 달라요.",
                "달러 결제($22) 구독인데 실제 청구 원화 금액과 차이가 나서 미납 알림이 뜨는 것 같습니다.",
                "2026-07-20", LocalDateTime.now().minusHours(3)));
    }

    private CustomerInquiryCategory findByName(List<CustomerInquiryCategory> categories, String name) {
        return categories.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }
}
