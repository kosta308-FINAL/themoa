package com.weaone.themoa.domain.auth.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.auth.dto.request.ChangePasswordRequest;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * 비밀번호 변경·전체 기기 로그아웃(auth.md §7-3): 성공 시 token_version이 올라가고 전 세션이 지워진다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceChangePasswordTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private AuthTokenService authTokenService;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Member member(String encodedPassword) {
        Member member = Member.signUp("user@example.com", encodedPassword, "닉네임", Gender.MALE,
                LocalDate.of(2000, 1, 1), LocalDateTime.now());
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    @Test
    @DisplayName("현재 비밀번호가 맞으면 비밀번호를 바꾸고 전 세션을 폐기한다")
    void changesPasswordAndRevokesAllSessions() {
        Member member = member("old-hash");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("old-pw", "old-hash")).willReturn(true);
        given(passwordEncoder.encode("new-pw-123!")).willReturn("new-hash");

        authService.changePassword(MEMBER_ID,
                new ChangePasswordRequest("old-pw", "new-pw-123!", "new-pw-123!"));

        assertThat(member.getPassword()).isEqualTo("new-hash");
        then(authTokenService).should().revokeAll(member);
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 거부하고 세션도 건드리지 않는다")
    void rejectsWrongCurrentPassword() {
        Member member = member("old-hash");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong-pw", "old-hash")).willReturn(false);

        assertThatThrownBy(() -> authService.changePassword(MEMBER_ID,
                new ChangePasswordRequest("wrong-pw", "new-pw-123!", "new-pw-123!")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
        then(authTokenService).should(org.mockito.Mockito.never()).revokeAll(any());
    }

    @Test
    @DisplayName("새 비밀번호 확인이 다르면 거부한다")
    void rejectsMismatchedConfirmation() {
        assertThatThrownBy(() -> authService.changePassword(MEMBER_ID,
                new ChangePasswordRequest("old-pw", "new-pw-123!", "different-123!")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_PASSWORD_CONFIRM_MISMATCH);
        then(memberRepository).should(org.mockito.Mockito.never()).findById(any());
    }

    @Test
    @DisplayName("전체 기기 로그아웃은 회원을 조회해 세션을 폐기한다")
    void logoutAllDevicesRevokesSessions() {
        Member member = member("hash");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        authService.logoutAllDevices(MEMBER_ID);

        then(authTokenService).should().revokeAll(member);
    }
}
