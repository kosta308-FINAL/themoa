package com.weaone.themoa.domain.auth.support;

/**
 * 아이디(이메일) 찾기 응답에 실제 이메일 대신 마스킹된 값을 내려주기 위한 유틸.
 * 로컬파트 앞 2자만 남기고 나머지는 길이를 노출하지 않도록 고정 길이(***)로 가린다.
 */
public final class EmailMasker {

    private EmailMasker() {
    }

    public static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        int visible = Math.min(2, local.length());
        return local.substring(0, visible) + "***" + domain;
    }
}
