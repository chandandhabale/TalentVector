package com.chandu.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import com.chandu.advisor.LoggingAdvisor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ChatConfig {

    // FIXED: Remove QuestionAnswerAdvisor from default advisors
    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, LoggingAdvisor loggingAdvisor) {
        return chatClientBuilder
                .defaultAdvisors(loggingAdvisor) // Only logging advisor
                .build();
    }
    
    // Keep this bean for RAG-specific endpoints (if needed)
    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore, SearchRequest searchRequest) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();
    }
    
    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel)
            .build();
        
        File outputFile = new File("src/main/resources/output-data/vectorstore.json");
        File inputFolder = new File("src/main/resources/input-data");
        
        // Check if we have a valid vector store file
        if (outputFile.exists() && outputFile.length() > 5000) {
            try {
                vectorStore.load(outputFile);
                log.info("Successfully loaded existing vector store with {} bytes", outputFile.length());
                
                // Test if vector store has content
                long docCount = vectorStore.similaritySearch(SearchRequest.builder()
                    .query("test")
                    .topK(1)
                    .build()).size();
                log.info("Vector store contains approximately {} documents", docCount);
                
                return vectorStore;
            } catch (Exception e) {
                log.warn("Failed to load vector store, will recreate: {}", e.getMessage());
            }
        }
        
        // Process documents with strict limits to avoid API quota issues
        if(!inputFolder.exists() || inputFolder.listFiles() == null) {
            log.warn("Input folder does not exist or is empty: {}", inputFolder.getAbsolutePath());
            return vectorStore;
        }
        
        List<Document> documents = new ArrayList<>();
        File[] files = inputFolder.listFiles();
        
        log.info("Starting document processing for {} files", files.length);
        
        // STRICT LIMITS to avoid quota issues
        int maxChunksPerFile = 5;  // Reduced from unlimited to 5 per file
        int totalChunksLimit = 20; // Overall limit of 20 chunks
        
        for (File file : files) {
            if (file.isFile() && documents.size() < totalChunksLimit) {
                try {
                    log.info("Processing file: {} (Size: {} bytes)", file.getName(), file.length());
                    
                    TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(file));
                    List<Document> rawDocuments = reader.get();
                    
                    log.info("Read {} raw documents from {}", rawDocuments.size(), file.getName());
                    
                    // Use TokenTextSplitter to create chunks
                    TokenTextSplitter splitter = new TokenTextSplitter();
                    List<Document> chunks = splitter.apply(rawDocuments);
                    
                    log.info("Split into {} chunks from {}", chunks.size(), file.getName());
                    
                    // Take only limited chunks to avoid API quota issues
                    List<Document> limitedChunks = chunks.stream()
                        .limit(maxChunksPerFile)
                        .collect(Collectors.toList());
                    
                    documents.addAll(limitedChunks);
                    
                    log.info("Successfully processed {} chunks from {}", limitedChunks.size(), file.getName());
                    
                    // Stop if we reach overall limit
                    if (documents.size() >= totalChunksLimit) {
                        log.info("Reached total chunks limit of {}", totalChunksLimit);
                        break;
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing file {}: {}", file.getName(), e.getMessage());
                }
            }
        }
        
        log.info("Total documents processed: {} chunks from all files", documents.size());
        
        if (!documents.isEmpty()) {
            try {
                log.info("Adding {} documents to vector store...", documents.size());
                vectorStore.add(documents);
                
                // Save the vector store
                vectorStore.save(outputFile);
                log.info("Vector store saved successfully with {} documents", documents.size());
                
                // Log document sources for verification
                logDocumentSources(documents);
                
            } catch (Exception e) {
                log.error("Error adding documents to vector store: {}", e.getMessage());
                // Don't throw exception - return empty vector store instead
                return SimpleVectorStore.builder(embeddingModel).build();
            }
        } else {
            log.warn("No documents were processed successfully");
        }
        
        return vectorStore;
    }
    
    private void logDocumentSources(List<Document> documents) {
        log.info("Document Sources Summary:");
        documents.stream()
            .map(doc -> doc.getMetadata().get("source"))
            .distinct()
            .forEach(source -> {
                long count = documents.stream()
                    .filter(doc -> source.equals(doc.getMetadata().get("source")))
                    .count();
                log.info("Source: {} - {} chunks", source, count);
            });
    }
    
    @Bean
    SearchRequest searchRequest() {
        return SearchRequest.builder()
                .similarityThreshold(0.5) 
                .topK(5)                  
                .build();
    }
}