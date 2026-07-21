package com.weaone.themoa.domain.customerservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_service_knowledge_chunk")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerKnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "knowledge_file_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CustomerKnowledgeFile knowledgeFile;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "qdrant_point_id", length = 36)
    private String qdrantPointId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private CustomerKnowledgeChunk(CustomerKnowledgeFile knowledgeFile, int chunkIndex, String content,
                                   LocalDateTime now) {
        this.knowledgeFile = knowledgeFile;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.createdAt = now;
    }

    public static CustomerKnowledgeChunk create(CustomerKnowledgeFile knowledgeFile, int chunkIndex, String content,
                                                LocalDateTime now) {
        return new CustomerKnowledgeChunk(knowledgeFile, chunkIndex, content, now);
    }

    public void assignQdrantPointId(String qdrantPointId) {
        this.qdrantPointId = qdrantPointId;
    }
}
