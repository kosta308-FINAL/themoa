package com.weaone.themoa.domain.notification.repository;

import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.notification.entity.Notification;
import com.weaone.themoa.domain.notification.entity.NotificationType;
import com.weaone.themoa.domain.notification.entity.NotificationTypeCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** 알림 목록·읽음 처리 조회 쿼리 검증(알림.md MOA-S-NOT-APP-02·-04). */
@SpringBootTest
@Transactional
class NotificationRepositoryIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationTypeRepository notificationTypeRepository;

    private Member persistMember(String email) {
        return memberRepository.save(
                Member.signUp(email, "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1), LocalDateTime.now()));
    }

    private NotificationType type(NotificationTypeCode code) {
        return notificationTypeRepository.findByCodeAndActiveTrue(code.name())
                .orElseGet(() -> notificationTypeRepository.save(
                        NotificationType.seed(code, code.name(), LocalDateTime.now())));
    }

    @Test
    @DisplayName("알림 목록은 최신순이고, 읽지 않은 알림 수는 다른 회원 알림을 세지 않는다")
    void listsNewestFirstAndCountsOwnUnreadOnly() {
        Member member = persistMember("noti-list@example.com");
        Member other = persistMember("noti-other@example.com");
        NotificationType missedPayment = type(NotificationTypeCode.MISSED_PAYMENT);
        NotificationType paymentDue = type(NotificationTypeCode.PAYMENT_DUE);

        Notification older = notificationRepository.save(Notification.create(member, missedPayment,
                "웨이브 결제가 확인되지 않았어요", null, null, "MISSED_PAYMENT:1", LocalDateTime.now().minusDays(1)));
        Notification newer = notificationRepository.save(Notification.create(member, paymentDue,
                "곧 결제 예정일이에요", null, null, "PAYMENT_DUE:1", LocalDateTime.now()));
        notificationRepository.save(Notification.create(other, paymentDue,
                "다른 회원 알림", null, null, "PAYMENT_DUE:2", LocalDateTime.now()));

        Page<Notification> page = notificationRepository.findByMember_IdOrderByCreatedAtDesc(member.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Notification::getId)
                .containsExactly(newer.getId(), older.getId());
        assertThat(notificationRepository.countByMember_IdAndReadFalse(member.getId())).isEqualTo(2);

        older.markRead(LocalDateTime.now());
        notificationRepository.saveAndFlush(older);

        assertThat(notificationRepository.countByMember_IdAndReadFalse(member.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 회원의 알림은 본인 조회로 찾을 수 없다")
    void ownershipScopedLookup() {
        Member member = persistMember("noti-owner@example.com");
        Member other = persistMember("noti-owner-other@example.com");
        NotificationType amountChange = type(NotificationTypeCode.AMOUNT_CHANGE);
        Notification mine = notificationRepository.save(Notification.create(member, amountChange,
                "구독료가 올랐어요", null, null, "AMOUNT_CHANGE:1", LocalDateTime.now()));

        assertThat(notificationRepository.findByIdAndMember_Id(mine.getId(), member.getId())).isPresent();
        assertThat(notificationRepository.findByIdAndMember_Id(mine.getId(), other.getId())).isEmpty();
    }
}
