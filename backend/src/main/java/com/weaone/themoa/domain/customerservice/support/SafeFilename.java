package com.weaone.themoa.domain.customerservice.support;

/**
 * 원본 파일명은 표시용 메타데이터로만 저장한다(customerservice.md §5). 제어문자·경로 구분자를 제거하고
 * 길이를 제한해 저장 경로 조립에 쓰이지 않아도 안전하게 표시할 수 있게 한다.
 */
public final class SafeFilename {

    private static final int MAX_LENGTH = 255;

    private SafeFilename() {
    }

    public static String sanitize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "attachment";
        }
        String stripped = originalFilename
                .replaceAll("[\\p{Cntrl}]", "")
                .replace("/", "_")
                .replace("\\", "_")
                .trim();
        if (stripped.isEmpty()) {
            return "attachment";
        }
        return stripped.length() > MAX_LENGTH ? stripped.substring(0, MAX_LENGTH) : stripped;
    }
}
