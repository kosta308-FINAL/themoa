package com.weaone.themoa.domain.logging.dto;

/**
 * Gemini 구조화 응답(managelogging.md §6-3). {@code causeCategory}는
 * {@code DATABASE|EXTERNAL_API|AUTH|NULL_POINTER|BUSINESS_LOGIC|CONFIGURATION|UNKNOWN} 중 하나로
 * System Prompt가 강제한다.
 */
public record AiDiagnosisDraft(
        String causeCategory,
        String summary,
        String rootCause,
        String recommendedAction
) {
}
