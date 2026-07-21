package com.weaone.themoa.domain.customerservice.rag;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomerKnowledgeDocumentChunker {

    private static final int MAX_CHUNK_LENGTH = 1_200;
    private static final int OVERLAP_LENGTH = 150;

    public List<String> chunk(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        for (String section : markdownSections(normalize(content))) {
            appendSection(chunks, section);
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

    private List<String> markdownSections(String content) {
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

    private void appendSection(List<String> chunks, String section) {
        if (section.length() <= MAX_CHUNK_LENGTH) {
            chunks.add(section);
            return;
        }
        StringBuilder current = new StringBuilder();
        for (String paragraph : section.split("\\n\\s*\\n")) {
            String trimmed = paragraph.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            if (trimmed.length() > MAX_CHUNK_LENGTH) {
                flush(chunks, current);
                splitLongParagraph(chunks, trimmed);
                continue;
            }
            if (current.length() + trimmed.length() + 2 > MAX_CHUNK_LENGTH) {
                flush(chunks, current);
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(trimmed);
        }
        flush(chunks, current);
    }

    private void splitLongParagraph(List<String> chunks, String paragraph) {
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, paragraph.length());
            chunks.add(paragraph.substring(start, end).trim());
            if (end == paragraph.length()) {
                break;
            }
            start = Math.max(0, end - OVERLAP_LENGTH);
        }
    }

    private void flush(List<String> chunks, StringBuilder current) {
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
            current.setLength(0);
        }
    }
}
