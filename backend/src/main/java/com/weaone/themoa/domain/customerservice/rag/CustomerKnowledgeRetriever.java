package com.weaone.themoa.domain.customerservice.rag;

import com.weaone.themoa.config.CustomerServiceRagProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CustomerKnowledgeRetriever {

    private final CustomerServiceRagProperties properties;
    private final CustomerKnowledgeDocumentProvider documentProvider;
    private final CustomerServiceVectorStoreFactory vectorStoreFactory;

    public CustomerKnowledgeRetriever(CustomerServiceRagProperties properties,
                                      CustomerKnowledgeDocumentProvider documentProvider,
                                      CustomerServiceVectorStoreFactory vectorStoreFactory) {
        this.properties = properties;
        this.documentProvider = documentProvider;
        this.vectorStoreFactory = vectorStoreFactory;
    }

    public List<CustomerKnowledgeSearchResult> retrieve(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        List<CustomerKnowledgeSearchResult> vectorResults = vectorResults(query);
        if (!vectorResults.isEmpty()) {
            return vectorResults;
        }
        return lexicalResults(query);
    }

    private List<CustomerKnowledgeSearchResult> vectorResults(String query) {
        if (!properties.enabled()) {
            return List.of();
        }
        VectorStore vectorStore = vectorStoreFactory.getIfAvailable();
        if (vectorStore == null) {
            return List.of();
        }
        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(properties.topK());
            if (properties.minimumSimilarity() > 0) {
                builder.similarityThreshold(properties.minimumSimilarity());
            }
            return vectorStore.similaritySearch(builder.build()).stream()
                    .map(this::fromVectorDocument)
                    .toList();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private CustomerKnowledgeSearchResult fromVectorDocument(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        CustomerKnowledgeSourceType sourceType = CustomerKnowledgeSourceType.valueOf(
                String.valueOf(metadata.getOrDefault("sourceType", CustomerKnowledgeSourceType.GUIDE.name())));
        CustomerKnowledgeDocument knowledgeDocument = new CustomerKnowledgeDocument(
                document.getId(),
                sourceType,
                String.valueOf(metadata.getOrDefault("sourceId", document.getId())),
                String.valueOf(metadata.getOrDefault("category", "고객센터")),
                String.valueOf(metadata.getOrDefault("title", "고객센터 안내")),
                document.getText());
        return new CustomerKnowledgeSearchResult(knowledgeDocument, document.getScore());
    }

    private List<CustomerKnowledgeSearchResult> lexicalResults(String query) {
        List<String> terms = terms(query);
        if (terms.isEmpty()) {
            return List.of();
        }
        return documentProvider.loadDocuments().stream()
                .map(document -> new CustomerKnowledgeSearchResult(document, lexicalScore(document, terms)))
                .filter(result -> result.score() != null && result.score() > 0)
                .sorted(Comparator.comparing(CustomerKnowledgeSearchResult::score).reversed())
                .limit(properties.topK())
                .toList();
    }

    private List<String> terms(String query) {
        return List.of(query.toLowerCase(Locale.ROOT).split("[^0-9a-zA-Z가-힣]+")).stream()
                .map(String::trim)
                .filter(term -> term.length() >= 2)
                .distinct()
                .toList();
    }

    private double lexicalScore(CustomerKnowledgeDocument document, List<String> terms) {
        String haystack = (document.title() + " " + document.category() + " " + document.content())
                .toLowerCase(Locale.ROOT);
        Map<String, Integer> matched = new LinkedHashMap<>();
        for (String term : terms) {
            if (haystack.contains(term)) {
                matched.put(term, term.length());
            }
        }
        return matched.values().stream().mapToInt(Integer::intValue).sum();
    }
}
