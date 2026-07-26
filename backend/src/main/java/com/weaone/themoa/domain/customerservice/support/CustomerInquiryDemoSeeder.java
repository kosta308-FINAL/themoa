package com.weaone.themoa.domain.customerservice.support;

import com.weaone.themoa.domain.customerservice.dto.request.InquiryAnswerRequest;
import com.weaone.themoa.domain.customerservice.dto.request.InquiryCreateRequest;
import com.weaone.themoa.domain.customerservice.dto.response.InquiryDetailResponse;
import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryCategory;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryCategoryRepository;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryRepository;
import com.weaone.themoa.domain.customerservice.service.AdminInquiryService;
import com.weaone.themoa.domain.customerservice.service.InquiryService;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.member.support.MemberDemoSeeder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 시연용 1:1 문의 5건 시드(member_id=1, email="test"). 실제 접수·답변 흐름과 동일하게
 * {@link InquiryService#create}와 {@link AdminInquiryService#upsertAnswer}를 그대로 호출해 상태·알림까지
 * 재현한다 — 4건은 관리자(email="admin")가 답변 완료 처리하고, 계정 보안 관련 1건은 PENDING으로 남겨
 * 관리자 문의 큐에서 미응답 케이스도 함께 보여줄 수 있게 한다.
 *
 * <p>{@link CustomerServiceMasterSeeder}(@Order 3)가 문의 카테고리를, {@link MemberDemoSeeder}
 * (@Order 4)가 demo·admin 계정을 먼저 만들어 둬야 하므로 그보다 뒤에 돈다.
 */
@Slf4j
@Component
@Order(12)
@RequiredArgsConstructor
public class CustomerInquiryDemoSeeder implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final CustomerInquiryCategoryRepository inquiryCategoryRepository;
    private final CustomerInquiryRepository inquiryRepository;
    private final InquiryService inquiryService;
    private final AdminInquiryService adminInquiryService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Member demo = memberRepository.findByEmail(MemberDemoSeeder.DEMO_EMAIL).orElse(null);
        Member admin = memberRepository.findByEmail(MemberDemoSeeder.ADMIN_EMAIL).orElse(null);
        if (demo == null || admin == null) {
            return;
        }
        if (inquiryRepository.existsByMember_Id(demo.getId())) {
            return;
        }

        Map<String, Long> categoryIdByName = inquiryCategoryRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()
                .stream()
                .collect(Collectors.toMap(CustomerInquiryCategory::getName, CustomerInquiryCategory::getId));

        submit(demo.getId(), categoryIdByName.get("카드 연동"),
                "국민카드 재연동 후에도 최근 결제내역이 안 보여요",
                "국민카드 비밀번호를 바꾸고 나서 재연동했는데, 최근 3일 결제내역이 계속 안 보여요. 확인 부탁드립니다.",
                admin.getId(), """
                        안녕하세요, 더모아 고객센터입니다. 확인 결과 카드사 서버 반영 지연으로 최근 내역 수집이 지연되었습니다.
                        지금은 정상적으로 수집이 완료된 상태이니 앱에서 새로고침해 확인해 주세요. 이용에 불편을 드려 죄송합니다.
                        """);

        submit(demo.getId(), categoryIdByName.get("고정지출"),
                "Claude 구독료가 매달 환율 때문에 예상 금액이랑 달라요",
                "고정지출로 등록한 Claude 구독(22달러)이 매달 원화 청구액이 조금씩 달라서 결제 금액 변경 알림이 계속 와요. 등록 금액을 바꿔야 하나요?",
                admin.getId(), """
                        해외 결제는 결제 시점 환율에 따라 원화 청구액이 매번 조금씩 달라질 수 있습니다.
                        차이가 ±10% 이내면 같은 결제로 자동 인식되니 별도 조치가 필요 없고, 계속 크게 벗어난다면
                        [고정지출] 상세에서 최근 청구액 기준으로 금액을 업데이트해 주세요.
                        """);

        submit(demo.getId(), categoryIdByName.get("일일 예산"),
                "월급일을 25일로 바꿨는데 이번 달 예산에 반영이 안 돼요",
                "이번 주기 중간에 월급일을 10일에서 25일로 변경했는데 하루 권장 소비액이 그대로예요. 잘못된 건가요?",
                admin.getId(), """
                        정상 동작입니다. 급여일을 변경해도 이미 시작된 급여주기는 그대로 유지되고,
                        변경한 급여일은 다음 급여주기부터 적용됩니다. 이번 주기가 끝나고 새 주기부터
                        25일 기준으로 반영되니 참고 부탁드립니다.
                        """);

        submit(demo.getId(), categoryIdByName.get("지출 직접 입력"),
                "카드 연동 전에 수기로 입력해둔 내역이 카드 내역이랑 중복돼서 잡혀요",
                "카드 연동하기 전에 현금처럼 수기로 입력해둔 카드 결제 몇 건이 있는데, 연동 이후에 같은 내역이 두 번 잡히는 것 같아요.",
                admin.getId(), """
                        카드를 연동하면 날짜·금액이 일치하는 수기 입력 건은 자동으로 카드사 수집 내역(정본)으로
                        대체되어 중복 집계되지 않습니다. 혹시 계속 두 번 잡히는 건이 있다면 가맹점명이 달라
                        매칭에 실패했을 가능성이 크니, 문의주신 결제일과 금액을 답장으로 알려주시면 직접 확인해드리겠습니다.
                        """);

        // 최신 문의 1건은 의도적으로 미응답(PENDING) 상태로 남겨 관리자 큐 데모에 사용한다.
        submit(demo.getId(), categoryIdByName.get("계정·보안"),
                "다른 사람이 제 계정에 로그인을 시도한 것 같아요",
                "어제 밤에 로그인을 5번 넘게 실패해서 계정이 잠겼었어요. 제가 시도한 게 아닌데 계정이 안전한지 확인 부탁드립니다.",
                null, null);

        log.info("데모 1:1 문의 5건 시드 완료(member_id={}, 답변 4건/미응답 1건)", demo.getId());
    }

    private void submit(Long memberId, Long categoryId, String title, String content,
                         Long adminId, String answerMarkdown) {
        InquiryCreateRequest request = new InquiryCreateRequest(categoryId, title, content, true);
        InquiryDetailResponse created = inquiryService.create(memberId, request, List.of());
        if (adminId != null) {
            adminInquiryService.upsertAnswer(adminId, created.id(), new InquiryAnswerRequest(answerMarkdown, null));
        }
    }
}
