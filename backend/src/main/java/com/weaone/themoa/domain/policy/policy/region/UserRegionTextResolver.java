package com.weaone.themoa.domain.policy.policy.region;

import com.weaone.themoa.domain.policy.policy.domain.RegionCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UserRegionTextResolver {
    private static final List<Pattern> RESIDENCE_BEFORE_PATTERNS = List.of(
            Pattern.compile("(.{0,24}?)(?:에|에서)?\\s*(?:살고|살아|사는\\s*곳|사는|거주|이사해서\\s*살고)"),
            Pattern.compile("(.{0,24}?)(?:집은|주소지|주민등록상|본가)")
    );
    private static final List<Pattern> RESIDENCE_AFTER_PATTERNS = List.of(
            Pattern.compile("(?:사는\\s*곳은?|집은|주소지는?|주민등록상|본가는?)\\s*(.{0,24})")
    );
    private static final List<Pattern> WORKPLACE_BEFORE_PATTERNS = List.of(
            Pattern.compile("(.{0,24}?)(?:로|으로|에서|에)?\\s*(?:출근|근무|일하고|일함|회사에\\s*다니|회사\\s*다니|직장에\\s*다니|직장\\s*다니|회사에서)"),
            Pattern.compile("(.{0,24}?)(?:회사가|직장이)")
    );
    private static final List<Pattern> WORKPLACE_AFTER_PATTERNS = List.of(
            Pattern.compile("(?:회사가|직장이|근무지는?|재직\\s*중인\\s*곳은?)\\s*(.{0,24})")
    );

    private final RegionCatalog regionCatalog;
    private final RegionNameAliasGenerator aliasGenerator;
    private final RegionNormalizer normalizer;

    @Autowired
    public UserRegionTextResolver(RegionCatalog regionCatalog, RegionNameAliasGenerator aliasGenerator, RegionNormalizer normalizer) {
        this.regionCatalog = regionCatalog;
        this.aliasGenerator = aliasGenerator;
        this.normalizer = normalizer;
    }

    public UserRegionTextResolver(RegionCatalog regionCatalog, RegionAliasCatalog aliases, RegionNormalizer normalizer) {
        this(regionCatalog, new RegionNameAliasGenerator(), normalizer);
    }

    public UserRegionResolution resolve(String text) {
        if (!StringUtils.hasText(text)) {
            return UserRegionResolution.notFound();
        }
        String compact = normalizer.compact(text);
        String nationwideMatch = nationwideMatch(compact);
        if (nationwideMatch != null) {
            return UserRegionResolution.nationwide(text.trim(), regionCatalog.nationwide().orElse(null), nationwideMatch);
        }
        List<RegionCode> regions = regionCatalog.allSpecificRegionsByLongestName();

        List<RegionTextMatchCandidate> candidates = new ArrayList<>();
        for (RegionCode region : regions) {
            candidates.addAll(matchCandidates(compact, region));
        }
        if (candidates.isEmpty()) {
            return UserRegionResolution.notFound();
        }
        return resolveByPriority(text.trim(), candidates);
    }

    public UserRegionContext resolveContext(String text) {
        if (!StringUtils.hasText(text)) {
            return UserRegionContext.empty();
        }
        List<UserRegionMention> mentions = new ArrayList<>();
        UserRegionResolution residence = firstRoleResolution(text, UserRegionRole.RESIDENCE, mentions);
        UserRegionResolution workplace = firstRoleResolution(text, UserRegionRole.WORKPLACE, mentions);

        if (!residence.resolved() && !workplace.resolved()) {
            UserRegionResolution fallback = resolve(text);
            if (fallback.resolved() || fallback.status() == UserRegionResolutionStatus.AMBIGUOUS) {
                mentions.add(new UserRegionMention(UserRegionRole.OTHER, fallback, fallback.matchedText(), 0, text.length()));
                residence = fallback;
            }
        }
        return new UserRegionContext(residence, workplace, mentions);
    }

    private UserRegionResolution firstRoleResolution(String text, UserRegionRole role, List<UserRegionMention> mentions) {
        List<Pattern> beforePatterns = role == UserRegionRole.RESIDENCE ? RESIDENCE_BEFORE_PATTERNS : WORKPLACE_BEFORE_PATTERNS;
        List<Pattern> afterPatterns = role == UserRegionRole.RESIDENCE ? RESIDENCE_AFTER_PATTERNS : WORKPLACE_AFTER_PATTERNS;
        if (role == UserRegionRole.RESIDENCE) {
            UserRegionResolution after = firstMatch(text, afterPatterns, role, mentions);
            if (after.resolved()) {
                return after;
            }
        }
        UserRegionResolution before = firstMatch(text, beforePatterns, role, mentions);
        if (before.resolved()) {
            return before;
        }
        UserRegionResolution after = firstMatch(text, afterPatterns, role, mentions);
        return after.resolved() ? after : UserRegionResolution.notFound();
    }

    private UserRegionResolution firstMatch(String text, List<Pattern> patterns, UserRegionRole role,
                                            List<UserRegionMention> mentions) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String evidence = matcher.group(1);
                UserRegionResolution resolution = resolve(evidence);
                if (resolution.resolved()) {
                    mentions.add(new UserRegionMention(role, resolution, evidence.trim(), matcher.start(1), matcher.end(1)));
                    return resolution;
                }
            }
        }
        return UserRegionResolution.notFound();
    }

    private String nationwideMatch(String compactText) {
        for (String expression : List.of("전국", "전국대상", "지역무관", "거주지무관", "지역제한없음", "거주지제한없음")) {
            if (compactText.contains(expression)) {
                return expression;
            }
        }
        return null;
    }

    private List<RegionTextMatchCandidate> matchCandidates(String compactText, RegionCode region) {
        List<RegionTextMatchCandidate> matches = new ArrayList<>();
        if ("PROVINCE".equals(region.getRegionLevel())) {
            addIfContains(matches, compactText, region, region.getProvince(), RegionTextMatchType.OFFICIAL_SIDO_NAME, 100);
            for (String alias : aliasGenerator.aliasesForSido(region)) {
                if (!alias.equals(region.getProvince())) {
                    addIfContains(matches, compactText, region, alias, RegionTextMatchType.GENERATED_SIDO_ALIAS, 80);
                }
            }
            return matches;
        }
        if (!StringUtils.hasText(region.getCity())) {
            return matches;
        }
        addIfContains(matches, compactText, region, region.displayName(), RegionTextMatchType.FULL_OFFICIAL_PATH, 110);
        addIfContains(matches, compactText, region, region.getCity(), RegionTextMatchType.OFFICIAL_SIGUNGU_NAME, 100);
        RegionCode sido = new RegionCode(null, "", region.getProvince(), null, "PROVINCE");
        for (String provinceAlias : aliasGenerator.aliasesForSido(sido)) {
            addIfContains(matches, compactText, region, provinceAlias + " " + region.getCity(),
                    RegionTextMatchType.FULL_OFFICIAL_PATH, 110);
            String shortCityAlias = aliasGenerator.shortAlias(region.getCity());
            if (StringUtils.hasText(shortCityAlias)) {
                addIfContains(matches, compactText, region, provinceAlias + " " + shortCityAlias,
                        RegionTextMatchType.SIDO_AND_SIGUNGU_ALIAS, 105);
            }
        }
        for (String alias : aliasGenerator.aliasesForSigungu(sido, region)) {
            if (alias.equals(region.getCity()) || alias.contains(" ")) {
                continue;
            }
            int priority = compactText.equals(normalizer.compact(alias)) ? 80 : 70;
            addIfContains(matches, compactText, region, alias, RegionTextMatchType.GENERATED_SIGUNGU_ALIAS, priority);
        }
        for (String generalDistrictAlias : regionCatalog.generalDistrictAliasesFor(region)) {
            addIfContains(matches, compactText, region, generalDistrictAlias, RegionTextMatchType.GENERATED_SIGUNGU_ALIAS, 80);
        }
        return matches;
    }

    private void addIfContains(List<RegionTextMatchCandidate> matches, String compactText, RegionCode region,
                               String matchedText, RegionTextMatchType matchType, int priority) {
        if (!StringUtils.hasText(matchedText)) {
            return;
        }
        String compactMatchedText = normalizer.compact(matchedText);
        if (StringUtils.hasText(compactMatchedText) && containsValidMatch(compactText, compactMatchedText, region, matchType)) {
            matches.add(new RegionTextMatchCandidate(region, matchType, matchedText.trim(), priority));
        }
    }

    private boolean containsValidMatch(String compactText, String compactMatchedText, RegionCode region, RegionTextMatchType matchType) {
        int index = compactText.indexOf(compactMatchedText);
        while (index >= 0) {
            if (matchType == RegionTextMatchType.FULL_OFFICIAL_PATH
                    || matchType == RegionTextMatchType.SIDO_AND_SIGUNGU_ALIAS
                    || SearchRegionLevel.from(region) == SearchRegionLevel.SIDO
                    || index == 0
                    || precededBySidoAlias(compactText, index, region)) {
                return true;
            }
            index = compactText.indexOf(compactMatchedText, index + 1);
        }
        return false;
    }

    private boolean precededBySidoAlias(String compactText, int matchIndex, RegionCode region) {
        if (SearchRegionLevel.from(region) != SearchRegionLevel.SIGUNGU) {
            return false;
        }
        RegionCode sido = new RegionCode(null, "", region.getProvince(), null, "PROVINCE");
        String prefix = compactText.substring(0, matchIndex);
        return aliasGenerator.aliasesForSido(sido).stream()
                .map(normalizer::compact)
                .anyMatch(prefix::endsWith);
    }

    private UserRegionResolution resolveByPriority(String rawRegionText, List<RegionTextMatchCandidate> candidates) {
        List<RegionTextMatchCandidate> distinct = logicalDistinctCandidates(candidates);
        int topPriority = distinct.stream().mapToInt(RegionTextMatchCandidate::priority).max().orElse(0);
        List<RegionTextMatchCandidate> top = distinct.stream()
                .filter(candidate -> candidate.priority() == topPriority)
                .sorted(Comparator.comparingInt((RegionTextMatchCandidate candidate) -> specificity(candidate.region())).reversed()
                        .thenComparing(candidate -> candidate.region().displayName().length(), Comparator.reverseOrder()))
                .toList();
        if (top.size() == 1) {
            return UserRegionResolution.of(statusFor(top.get(0)), rawRegionText, top.get(0));
        }
        UserRegionResolution sameSidoDefault = resolveSameSidoDefaultAlias(rawRegionText, top);
        if (sameSidoDefault != null) {
            return sameSidoDefault;
        }
        return UserRegionResolution.ambiguous(rawRegionText, top);
    }

    private UserRegionResolution resolveSameSidoDefaultAlias(String rawRegionText, List<RegionTextMatchCandidate> top) {
        List<RegionTextMatchCandidate> sidos = top.stream()
                .filter(candidate -> SearchRegionLevel.from(candidate.region()) == SearchRegionLevel.SIDO)
                .toList();
        if (sidos.size() != 1) {
            return null;
        }
        RegionCode sido = sidos.get(0).region();
        boolean allWithinSido = top.stream().allMatch(candidate -> sido.getProvince().equals(candidate.region().getProvince()));
        boolean generatedOnly = top.stream().allMatch(candidate -> candidate.matchType() == RegionTextMatchType.GENERATED_SIDO_ALIAS
                || candidate.matchType() == RegionTextMatchType.GENERATED_SIGUNGU_ALIAS);
        if (allWithinSido && generatedOnly) {
            return UserRegionResolution.of(UserRegionResolutionStatus.UNIQUE_ALIAS, rawRegionText, sidos.get(0));
        }
        return null;
    }

    private UserRegionResolutionStatus statusFor(RegionTextMatchCandidate candidate) {
        return switch (candidate.matchType()) {
            case FULL_OFFICIAL_PATH, OFFICIAL_SIDO_NAME, OFFICIAL_SIGUNGU_NAME, SIDO_AND_SIGUNGU_ALIAS ->
                    UserRegionResolutionStatus.EXACT;
            case GENERATED_SIDO_ALIAS, GENERATED_SIGUNGU_ALIAS -> UserRegionResolutionStatus.UNIQUE_ALIAS;
        };
    }

    private List<RegionTextMatchCandidate> logicalDistinctCandidates(List<RegionTextMatchCandidate> candidates) {
        return new ArrayList<>(candidates.stream()
                .collect(java.util.stream.Collectors.toMap(
                        candidate -> logicalKey(candidate.region()),
                        candidate -> candidate,
                        this::choosePreferredCandidate,
                        LinkedHashMap::new))
                .values());
    }

    private RegionTextMatchCandidate choosePreferredCandidate(RegionTextMatchCandidate left, RegionTextMatchCandidate right) {
        if (right.priority() > left.priority()) {
            return right;
        }
        if (right.priority() < left.priority()) {
            return left;
        }
        RegionCode preferredRegion = choosePreferred(left.region(), right.region());
        if (preferredRegion == right.region()) {
            return right;
        }
        return left;
    }

    private RegionCode choosePreferred(RegionCode left, RegionCode right) {
        if (standardInternalCode(right) && !standardInternalCode(left)) {
            return right;
        }
        return left;
    }

    private boolean standardInternalCode(RegionCode region) {
        String code = region.getRegionCode();
        return "KR".equals(code) || code.startsWith("P:") || code.startsWith("M:");
    }

    private String logicalKey(RegionCode region) {
        return SearchRegionLevel.from(region) + "|" + region.getProvince() + "|" + (region.getCity() == null ? "" : region.getCity());
    }

    private int specificity(RegionCode region) {
        return switch (region.getRegionLevel()) {
            case "CITY" -> 2;
            case "DISTRICT" -> 2;
            case "PROVINCE" -> 1;
            default -> 0;
        };
    }
}
