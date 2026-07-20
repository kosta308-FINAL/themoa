package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.UserApplicantType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserApplicantTypeDetector {
    public UserApplicantType detect(String query) {
        if (!StringUtils.hasText(query)) {
            return UserApplicantType.UNKNOWN;
        }
        String text = query.replaceAll("\\s+", "");
        if (containsAny(text,
                "우리회사", "우리기업", "법인", "사업주", "고용주", "직원을채용", "청년을채용",
                "채용하려고", "참여기업", "수요기업", "기업지원사업", "사업장지원",
                "기업이받을수있는", "기업으로신청", "사업주고용장려금")) {
            return UserApplicantType.ORGANIZATION;
        }
        if (containsAny(text,
                "나는", "나에게", "내가", "청년인데", "직장인", "회사에다니", "회사다니",
                "직장에다니", "직장다니", "대학생", "취준생", "백수", "고3", "고등학생",
                "수원에거주", "거주", "살고", "살아", "출근", "나이", "살", "세",
                "재직", "무직", "미취업", "구직중", "받을수있는")) {
            return UserApplicantType.INDIVIDUAL;
        }
        return UserApplicantType.UNKNOWN;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
