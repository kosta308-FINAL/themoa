package com.weaone.themoa.domain.customerservice.rag;

public record CustomerKnowledgeChunkingOptions(
        int maxChunkLength,
        int overlapLength,
        boolean splitByMarkdownHeading,
        boolean splitByParagraph
) {

    public static final int DEFAULT_MAX_CHUNK_LENGTH = 1_200;
    public static final int DEFAULT_OVERLAP_LENGTH = 150;
    public static final boolean DEFAULT_SPLIT_BY_MARKDOWN_HEADING = true;
    public static final boolean DEFAULT_SPLIT_BY_PARAGRAPH = false;

    private static final int MIN_CHUNK_LENGTH = 300;
    private static final int MAX_CHUNK_LENGTH = 4_000;
    private static final int MIN_OVERLAP_LENGTH = 0;
    private static final int MAX_OVERLAP_LENGTH = 800;

    public static CustomerKnowledgeChunkingOptions defaults() {
        return new CustomerKnowledgeChunkingOptions(
                DEFAULT_MAX_CHUNK_LENGTH,
                DEFAULT_OVERLAP_LENGTH,
                DEFAULT_SPLIT_BY_MARKDOWN_HEADING,
                DEFAULT_SPLIT_BY_PARAGRAPH);
    }

    public static CustomerKnowledgeChunkingOptions normalize(Integer maxChunkLength, Integer overlapLength,
                                                             Boolean splitByMarkdownHeading, Boolean splitByParagraph) {
        int normalizedMax = clamp(
                maxChunkLength == null ? DEFAULT_MAX_CHUNK_LENGTH : maxChunkLength,
                MIN_CHUNK_LENGTH,
                MAX_CHUNK_LENGTH);
        int normalizedOverlap = clamp(
                overlapLength == null ? DEFAULT_OVERLAP_LENGTH : overlapLength,
                MIN_OVERLAP_LENGTH,
                Math.min(MAX_OVERLAP_LENGTH, normalizedMax / 2 - 1));
        boolean normalizedHeadingSplit = splitByMarkdownHeading == null
                ? DEFAULT_SPLIT_BY_MARKDOWN_HEADING
                : splitByMarkdownHeading;
        boolean normalizedParagraphSplit = splitByParagraph == null
                ? DEFAULT_SPLIT_BY_PARAGRAPH
                : splitByParagraph;
        return new CustomerKnowledgeChunkingOptions(
                normalizedMax, normalizedOverlap, normalizedHeadingSplit, normalizedParagraphSplit);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
