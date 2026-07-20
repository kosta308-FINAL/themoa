package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.RecommendationTier;

import java.util.List;

public record PolicyRankingEvaluation(
        double topicScore,
        double rawSemanticScore,
        double normalizedSemanticScore,
        double weightedSemanticScore,
        double semanticScore,
        double lexicalScore,
        double titleScore,
        double domainScore,
        double supportIntentScore,
        double eligibilityBreadthScore,
        String eligibilityBreadth,
        List<String> eligibilityBreadthEvidence,
        double regionScore,
        double rawFinalScore,
        double finalScore,
        RecommendationTier recommendationTier,
        int finalRank,
        List<String> rankingReasons
) {
    public PolicyRankingEvaluation(double topicScore,
                                   double semanticScore,
                                   double lexicalScore,
                                   double titleScore,
                                   double domainScore,
                                   double supportIntentScore,
                                   double regionScore,
                                   double finalScore,
                                   RecommendationTier recommendationTier,
                                   int finalRank,
                                   List<String> rankingReasons) {
        this(topicScore, semanticScore, semanticScore, semanticScore * 0.35, semanticScore, lexicalScore, titleScore,
                domainScore, supportIntentScore, 0.5, "UNKNOWN", List.of(), regionScore, finalScore, finalScore, recommendationTier, finalRank,
                rankingReasons);
    }

    public PolicyRankingEvaluation {
        eligibilityBreadth = eligibilityBreadth == null ? "UNKNOWN" : eligibilityBreadth;
        eligibilityBreadthEvidence = eligibilityBreadthEvidence == null ? List.of() : List.copyOf(eligibilityBreadthEvidence);
        rankingReasons = rankingReasons == null ? List.of() : List.copyOf(rankingReasons);
    }

    public PolicyRankingEvaluation(double topicScore,
                                   double semanticScore,
                                   double lexicalScore,
                                   double titleScore,
                                   double domainScore,
                                   double supportIntentScore,
                                   double regionScore,
                                   double rawFinalScore,
                                   double finalScore,
                                   RecommendationTier recommendationTier,
                                   int finalRank,
                                   List<String> rankingReasons) {
        this(topicScore, semanticScore, semanticScore, semanticScore * 0.35, semanticScore, lexicalScore, titleScore, domainScore, supportIntentScore,
                0.5, "UNKNOWN", List.of(), regionScore, rawFinalScore, finalScore, recommendationTier, finalRank,
                rankingReasons);
    }
}
