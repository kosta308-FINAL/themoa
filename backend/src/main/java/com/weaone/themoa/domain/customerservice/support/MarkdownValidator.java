package com.weaone.themoa.domain.customerservice.support;

import java.util.regex.Pattern;

/** 답변 Markdown 원문에 raw HTML을 허용하지 않는다(customerservice.md §0). */
public final class MarkdownValidator {

    private static final Pattern HTML_TAG = Pattern.compile("<\\s*/?\\s*[a-zA-Z][^>]*>");

    private MarkdownValidator() {
    }

    public static boolean containsRawHtml(String markdown) {
        return HTML_TAG.matcher(markdown).find();
    }
}
