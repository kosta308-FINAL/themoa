package com.weaone.themoa.domain.customerservice.rag;

import com.weaone.themoa.config.CustomerServiceRagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@Order(20)
public class CustomerKnowledgeEmbeddingService implements ApplicationRunner {

    private final CustomerServiceRagProperties properties;
    private final CustomerKnowledgeDocumentProvider documentProvider;
    private final CustomerServiceVectorStoreFactory vectorStoreFactory;

    public CustomerKnowledgeEmbeddingService(CustomerServiceRagProperties properties,
                                             CustomerKnowledgeDocumentProvider documentProvider,
                                             CustomerServiceVectorStoreFactory vectorStoreFactory) {
        this.properties = properties;
        this.documentProvider = documentProvider;
        this.vectorStoreFactory = vectorStoreFactory;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.embedOnStartup()) {
            embedAll();
        }
    }

    public int embedAll() {
        if (!properties.enabled()) {
            log.info("Customer service RAG is disabled.");
            return 0;
        }
        VectorStore vectorStore = vectorStoreFactory.getIfAvailable();
        if (vectorStore == null) {
            log.warn("Customer service VectorStore is not available. Skip startup embedding.");
            return 0;
        }
        List<CustomerKnowledgeDocument> knowledgeDocuments = documentProvider.loadDocuments();
        List<Document> documents = knowledgeDocuments.stream()
                .map(this::toVectorDocument)
                .toList();
        if (documents.isEmpty()) {
            return 0;
        }
        try {
            vectorStore.add(documents);
            log.info("Embedded {} customer service knowledge documents into Qdrant collection '{}'.",
                    documents.size(), properties.collectionName());
            return documents.size();
        } catch (RuntimeException ex) {
            log.warn("Customer service knowledge embedding failed. Chat will use lexical fallback.", ex);
            return 0;
        }
    }

    private Document toVectorDocument(CustomerKnowledgeDocument document) {
        return Document.builder()
                .id(stableUuid(document.id()))
                .text(document.content())
                .metadata(Map.of(
                        "sourceType", document.sourceType().name(),
                        "sourceId", document.sourceId(),
                        "knowledgeId", document.id(),
                        "category", document.category(),
                        "title", document.title()))
                .build();
    }

    private String stableUuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
