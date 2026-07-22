package com.weaone.themoa.domain.customerservice.rag;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomerKnowledgeDocumentChunker {

    public List<String> chunk(String content) {
        return chunk(content, CustomerKnowledgeChunkingOptions.defaults());
    }

    public List<String> chunk(String content, CustomerKnowledgeChunkingOptions options) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        CustomerKnowledgeChunkingOptions normalizedOptions = options == null
                ? CustomerKnowledgeChunkingOptions.defaults()
                : options;
        List<String> chunks = new ArrayList<>();
        for (String section : sections(normalize(content), normalizedOptions.splitByMarkdownHeading())) {
            appendSection(chunks, section, normalizedOptions);
        }
        return chunks.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String normalize(String content) {
        return content.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\uFEFF", "")
                .trim();
    }

    private List<String> sections(String content, boolean splitByMarkdownHeading) {
        if (!splitByMarkdownHeading) {
            return List.of(content);
        }
        List<String> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : content.split("\n")) {
            boolean heading = line.matches("^#{1,6}\\s+.+");
            if (heading && !current.isEmpty()) {
                sections.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(line).append('\n');
        }
        if (!current.isEmpty()) {
            sections.add(current.toString().trim());
        }
        return sections;
    }

    private void appendSection(List<String> chunks, String section, CustomerKnowledgeChunkingOptions options) {
        if (section.length() <= options.maxChunkLength() && !options.splitByParagraph()) {
            chunks.add(section);
            return;
        }
        List<String> sectionChunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : section.split("\\n\\s*\\n")) {
            String trimmed = paragraph.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            if (trimmed.length() > options.maxChunkLength()) {
                flush(sectionChunks, current);
                splitLongParagraph(sectionChunks, trimmed, options);
                continue;
            }
            if (options.splitByParagraph()) {
                flush(sectionChunks, current);
                sectionChunks.add(trimmed);
                continue;
            }
            if (current.length() + trimmed.length() + 2 > options.maxChunkLength()) {
                flush(sectionChunks, current);
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(trimmed);
        }
        flush(sectionChunks, current);
        chunks.addAll(mergeShortChunks(sectionChunks, options));
    }

    private List<String> mergeShortChunks(List<String> sectionChunks, CustomerKnowledgeChunkingOptions options) {
        if (options.minChunkLength() <= 0 || sectionChunks.size() < 2) {
            return sectionChunks;
        }
        List<String> merged = new ArrayList<>();
        for (String chunk : sectionChunks) {
            if (!merged.isEmpty()) {
                String previous = merged.get(merged.size() - 1);
                boolean previousTooShort = previous.length() < options.minChunkLength();
                boolean fitsTogether = previous.length() + chunk.length() + 2 <= options.maxChunkLength();
                if (previousTooShort && fitsTogether) {
                    merged.set(merged.size() - 1, previous + "\n\n" + chunk);
                    continue;
                }
            }
            merged.add(chunk);
        }
        int lastIndex = merged.size() - 1;
        if (lastIndex > 0 && merged.get(lastIndex).length() < options.minChunkLength()) {
            String last = merged.get(lastIndex);
            String previous = merged.get(lastIndex - 1);
            if (previous.length() + last.length() + 2 <= options.maxChunkLength()) {
                merged.set(lastIndex - 1, previous + "\n\n" + last);
                merged.remove(lastIndex);
            }
        }
        return merged;
    }

    private void splitLongParagraph(List<String> chunks, String paragraph, CustomerKnowledgeChunkingOptions options) {
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + options.maxChunkLength(), paragraph.length());
            chunks.add(paragraph.substring(start, end).trim());
            if (end == paragraph.length()) {
                break;
            }
            start = Math.max(0, end - options.overlapLength());
        }
    }

    private void flush(List<String> chunks, StringBuilder current) {
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
            current.setLength(0);
        }
    }
}
