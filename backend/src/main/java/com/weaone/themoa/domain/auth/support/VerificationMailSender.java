package com.weaone.themoa.domain.auth.support;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.config.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 인증 코드 메일 발송(Gmail SMTP). 코드·수신 이메일은 로그에 남기지 않는다.
 */
@Slf4j
@Component
public class VerificationMailSender {

    private static final String SUBJECT = "[더모아] 이메일 인증 코드";
    private static final String PASSWORD_RESET_SUBJECT = "[더모아] 비밀번호 재설정 인증 코드";

    private final JavaMailSender mailSender;
    private final String from;
    private final Duration codeTtl;

    public VerificationMailSender(JavaMailSender mailSender, AuthProperties properties) {
        this.mailSender = mailSender;
        this.from = properties.emailVerification().from();
        this.codeTtl = properties.emailVerification().codeTtl();
    }

    public void send(String email, String code) {
        sendMessage(email, SUBJECT, buildBody(code));
    }

    /** 비밀번호 찾기 흐름 전용 메일. 본문에서 목적을 명확히 밝혀 회원가입 인증 메일과 혼동하지 않게 한다. */
    public void sendPasswordResetCode(String email, String code) {
        sendMessage(email, PASSWORD_RESET_SUBJECT, buildPasswordResetBody(code));
    }

    private void sendMessage(String email, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("인증 메일 발송 실패", e);
            throw new BusinessException(ErrorCode.AUTH_EMAIL_SEND_FAILED);
        }
    }

    private String buildBody(String code) {
        return """
                아래 인증 코드를 입력해 주세요.

                인증 코드: %s

                유효 시간은 %d분입니다.
                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(code, codeTtl.toMinutes());
    }

    private String buildPasswordResetBody(String code) {
        return """
                비밀번호 재설정을 위한 인증 코드입니다.

                인증 코드: %s

                유효 시간은 %d분입니다.
                본인이 요청하지 않았다면 이 메일을 무시해 주세요. 비밀번호는 변경되지 않습니다.
                """.formatted(code, codeTtl.toMinutes());
    }
}