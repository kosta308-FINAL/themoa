package com.weaone.themoa.domain.customerservice.rag;

import com.weaone.themoa.config.CustomerServiceRagProperties;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
        List<CustomerKnowledgeSearchResult> vectorResults = vectorResults(query, properties.topK(),
                properties.minimumSimilarity());
        if (!vectorResults.isEmpty()) {
            return vectorResults;
        }
        log.info("Customer service RAG Qdrant result is empty. Use lexical fallback. query='{}'", query);
        return lexicalResults(query, properties.topK());
    }

    public List<CustomerKnowledgeSearchResult> retrieve(String query, int topK, double minimumSimilarity) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        List<CustomerKnowledgeSearchResult> vectorResults = vectorResults(query, topK, minimumSimilarity);
        if (!vectorResults.isEmpty()) {
            return vectorResults;
        }
        log.info("Customer service RAG Qdrant result is empty. Use lexical fallback. query='{}'", query);
        return lexicalResults(query, topK);
    }

    public List<CustomerKnowledgeSearchResult> retrieveVectorOnly(String query, int topK, double minimumSimilarity) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        return vectorResults(query, topK, minimumSimilarity);
    }

    private List<CustomerKnowledgeSearchResult> vectorResults(String query, int topK, double minimumSimilarity) {
        if (!properties.enabled()) {
            log.info("Customer service RAG is disabled. Skip Qdrant search. query='{}'", query);
            return List.of();
        }
        VectorStore vectorStore = vectorStoreFactory.getIfAvailable();
        if (vectorStore == null) {
            log.info("Customer service VectorStore is not available. Skip Qdrant search. query='{}'", query);
            return List.of();
        }
        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(topK);
            if (minimumSimilarity > 0) {
                builder.similarityThreshold(minimumSimilarity);
            }
            List<CustomerKnowledgeSearchResult> results = vectorStore.similaritySearch(builder.build()).stream()
                    .map(this::fromVectorDocument)
                    .toList();
            logVectorResults(query, topK, minimumSimilarity, results);
            return results;
        } catch (RuntimeException ex) {
            log.warn("Customer service Qdrant search failed. query='{}'", query, ex);
            return List.of();
        }
    }

    private void logVectorResults(String query, int topK, double minimumSimilarity,
                                  List<CustomerKnowledgeSearchResult> results) {
        log.info("Customer service Qdrant search. collection='{}', topK={}, minimumSimilarity={}, query='{}', resultCount={}",
                properties.collectionName(), topK, minimumSimilarity, query, results.size());
        for (int index = 0; index < results.size(); index++) {
            CustomerKnowledgeSearchResult result = results.get(index);
            CustomerKnowledgeDocument document = result.document();
            log.info("Customer service Qdrant result #{}: score={}, sourceType={}, sourceId={}, category='{}', title='{}', content='{}'",
                    index + 1,
                    result.score(),
                    document.sourceType(),
                    document.sourceId(),
                    document.category(),
                    document.title(),
                    abbreviate(document.content(), 800));
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

    private List<CustomerKnowledgeSearchResult> lexicalResults(String query, int topK) {
        List<String> terms = terms(query);
        if (terms.isEmpty()) {
            return List.of();
        }
        return documentProvider.loadDocuments().stream()
                .map(document -> new CustomerKnowledgeSearchResult(document, lexicalScore(document, terms)))
                .filter(result -> result.score() != null && result.score() > 0)
                .sorted(Comparator.comparing(CustomerKnowledgeSearchResult::score).reversed())
                .limit(topK)
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

    private String abbreviate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }
}
