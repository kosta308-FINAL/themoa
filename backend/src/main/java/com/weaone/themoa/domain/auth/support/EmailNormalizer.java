package com.weaone.themoa.domain.auth.support;

import java.util.Locale;

/**
 * 이메일은 로그인 식별자이자 UNIQUE 키다. 가입·로그인·인증이 같은 규칙으로 정규화해야
 * "대문자로 가입하고 소문자로 로그인" 같은 불일치가 생기지 않는다.
 */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}