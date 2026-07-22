package com.weaone.themoa.domain.customerservice.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_service_knowledge_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerKnowledgeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "chunk_max_length")
    private Integer chunkMaxLength;

    @Column(name = "chunk_overlap_length")
    private Integer chunkOverlapLength;

    @Column(name = "split_by_markdown_heading")
    private Boolean splitByMarkdownHeading;

    @Column(name = "split_by_paragraph")
    private Boolean splitByParagraph;

    @Lob
    @Column(name = "original_content", nullable = false, columnDefinition = "LONGTEXT")
    private String originalContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerKnowledgeFileStatus status;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Member createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "embedded_at")
    private LocalDateTime embeddedAt;

    private CustomerKnowledgeFile(String title, String category, String originalFilename, long fileSize,
                                  String originalContent, int chunkMaxLength, int chunkOverlapLength,
                                  boolean splitByMarkdownHeading, boolean splitByParagraph, Member createdBy,
                                  LocalDateTime now) {
        this.title = title;
        this.category = category;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.chunkMaxLength = chunkMaxLength;
        this.chunkOverlapLength = chunkOverlapLength;
        this.splitByMarkdownHeading = splitByMarkdownHeading;
        this.splitByParagraph = splitByParagraph;
        this.originalContent = originalContent;
        this.status = CustomerKnowledgeFileStatus.FAILED;
        this.active = true;
        this.createdBy = createdBy;
        this.createdAt = now;
    }

    public static CustomerKnowledgeFile create(String title, String category, String originalFilename, long fileSize,
                                               String originalContent, int chunkMaxLength, int chunkOverlapLength,
                                               boolean splitByMarkdownHeading, boolean splitByParagraph,
                                               Member createdBy, LocalDateTime now) {
        return new CustomerKnowledgeFile(
                title,
                category,
                originalFilename,
                fileSize,
                originalContent,
                chunkMaxLength,
                chunkOverlapLength,
                splitByMarkdownHeading,
                splitByParagraph,
                createdBy,
                now);
    }

    public int getChunkMaxLength() {
        return chunkMaxLength == null ? 1_200 : chunkMaxLength;
    }

    public int getChunkOverlapLength() {
        return chunkOverlapLength == null ? 150 : chunkOverlapLength;
    }

    public boolean isSplitByMarkdownHeading() {
        return splitByMarkdownHeading == null || splitByMarkdownHeading;
    }

    public boolean isSplitByParagraph() {
        return splitByParagraph != null && splitByParagraph;
    }

    public void updateChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public void markEmbedded(LocalDateTime now) {
        this.status = CustomerKnowledgeFileStatus.EMBEDDED;
        this.embeddedAt = now;
        this.active = true;
    }

    public void markFailed() {
        this.status = CustomerKnowledgeFileStatus.FAILED;
        this.embeddedAt = null;
    }

    public void disable() {
        this.status = CustomerKnowledgeFileStatus.DISABLED;
        this.active = false;
    }
}
