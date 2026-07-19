package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.PolicyTitleIdentity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PolicyKeywordNormalizer {
    private static final Pattern PARENTHESIS = Pattern.compile("\\(([^)]+)\\)|\\[([^]]+)]|（([^）]+)）");

    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
                .replaceAll("[\\s\\-\\u2010\\u2011\\u2012\\u2013\\u2014\\u2212·ㆍ,()\\[\\]{}<>\"'`~!@#$%^&*_=+|\\\\:;?/.]", "");
    }

    public PolicyTitleIdentity titleIdentity(String rawTitle) {
        String displayTitle = StringUtils.hasText(rawTitle) ? rawTitle.trim() : "";
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        if (StringUtils.hasText(displayTitle)) {
            aliases.add(displayTitle);
        }
        String canonical = canonicalTitle(displayTitle);
        if (StringUtils.hasText(canonical)) {
            aliases.add(canonical);
            aliases.add(canonical.replace("-", ""));
            aliases.add(canonical.replaceAll("\\s+", ""));
        }
        Matcher matcher = PARENTHESIS.matcher(displayTitle);
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String alias = matcher.group(i);
                if (StringUtils.hasText(alias)) {
                    aliases.add(alias.trim());
                    aliases.add(alias.replace("-", "").trim());
                    aliases.add(alias.replaceAll("\\s+", "").trim());
                }
            }
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        aliases.stream()
                .map(this::normalize)
                .filter(value -> value.length() >= 2)
                .forEach(normalized::add);
        if (StringUtils.hasText(canonical)) {
            normalized.add(normalize(canonical));
        }
        return new PolicyTitleIdentity(displayTitle, canonical, aliases, normalized);
    }

    public boolean exactTitleMatch(String query, String rawTitle) {
        String normalizedQuery = normalize(query);
        if (!StringUtils.hasText(normalizedQuery)) {
            return false;
        }
        return titleIdentity(rawTitle).normalizedAliases().contains(normalizedQuery);
    }

    private String canonicalTitle(String displayTitle) {
        if (!StringUtils.hasText(displayTitle)) {
            return "";
        }
        String trimmed = displayTitle.trim();
        int parenIndex = firstParenIndex(trimmed);
        if (parenIndex > 0) {
            return trimmed.substring(0, parenIndex).trim();
        }
        String[] separators = {" - ", " : ", " | "};
        for (String separator : separators) {
            int index = trimmed.indexOf(separator);
            if (index > 0) {
                return trimmed.substring(0, index).trim();
            }
        }
        return trimmed;
    }

    private int firstParenIndex(String value) {
        int round = value.indexOf('(');
        int square = value.indexOf('[');
        int full = value.indexOf('（');
        int result = -1;
        for (int index : new int[]{round, square, full}) {
            if (index >= 0 && (result < 0 || index < result)) {
                result = index;
            }
        }
        return result;
    }
}
