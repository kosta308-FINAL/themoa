package com.weaone.themoa.domain.customerservice.support;

/**
 * 파일 시그니처(매직 바이트) 검증(customerservice.md §5). 확장자·클라이언트 Content-Type만 믿지 않고
 * 실제 바이트로 PNG·JPEG만 허용한다. SVG·GIF·WebP·실행 파일·압축 파일은 이 검증을 통과하지 못한다.
 */
public final class ImageSignatureValidator {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private ImageSignatureValidator() {
    }

    /** 인식되지 않는 형식이면 null을 반환한다. */
    public static String detectContentType(byte[] content) {
        if (matches(content, PNG_SIGNATURE)) {
            return "image/png";
        }
        if (matches(content, JPEG_SIGNATURE)) {
            return "image/jpeg";
        }
        return null;
    }

    public static String extensionFor(String contentType) {
        return "image/png".equals(contentType) ? "png" : "jpg";
    }

    private static boolean matches(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
