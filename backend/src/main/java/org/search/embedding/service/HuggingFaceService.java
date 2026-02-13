package org.search.embedding.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

/**
 * Service for generating embeddings using HuggingFace models
 * Provides generic embeddings for comparison with custom model
 */
@Service
public class HuggingFaceService {

    private static final Logger logger = LoggerFactory.getLogger(HuggingFaceService.class);

    private static final String MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2";
    
    /**
     * Calculate similarity using HuggingFace sentence transformers
     * This is a simplified implementation - for production use, consider using
     * the actual sentence-transformers library or a REST API
     */
    public float calculateGenericSimilarity(String word1, String word2) {
        try {
            // For demonstration, we'll use a simple approach
            // In production, you would load a proper sentence transformer model
            float[] emb1 = getSimpleEmbedding(word1);
            float[] emb2 = getSimpleEmbedding(word2);
            
            return cosineSimilarity(emb1, emb2);
        } catch (Exception e) {
            logger.error("Error calculating generic similarity: {}", e.getMessage(), e);
            // Fallback to simple string similarity
            return simpleStringSimilarity(word1, word2);
        }
    }
    
    /**
     * Simple embedding based on character-level features
     * This is a placeholder - in production, use actual transformer models
     */
    private float[] getSimpleEmbedding(String word) {
        // Simple character-based embedding (for demo purposes)
        float[] embedding = new float[128];
        
        char[] chars = word.toLowerCase().toCharArray();
        for (int i = 0; i < chars.length && i < 32; i++) {
            int idx = chars[i] % 128;
            embedding[idx] += 1.0f;
        }
        
        // Add bigram features
        for (int i = 0; i < chars.length - 1 && i < 16; i++) {
            int idx = ((chars[i] + chars[i+1]) % 64) + 64;
            embedding[idx] += 0.5f;
        }
        
        // Normalize
        float norm = 0f;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
        }
        
        return embedding;
    }
    
    /**
     * Calculate cosine similarity between two vectors
     */
    private float cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }
        
        float dotProduct = 0f;
        float norm1 = 0f;
        float norm2 = 0f;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        float denominator = (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
        if (denominator == 0) {
            return 0f;
        }
        
        return dotProduct / denominator;
    }
    
    /**
     * Fallback: Simple string similarity (Jaccard coefficient of character sets)
     */
    private float simpleStringSimilarity(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        
        // Jaccard similarity of character sets
        java.util.Set<Character> set1 = new java.util.HashSet<>();
        java.util.Set<Character> set2 = new java.util.HashSet<>();
        
        for (char c : s1.toCharArray()) set1.add(c);
        for (char c : s2.toCharArray()) set2.add(c);
        
        java.util.Set<Character> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);
        
        java.util.Set<Character> union = new java.util.HashSet<>(set1);
        union.addAll(set2);
        
        if (union.isEmpty()) return 0f;
        
        return (float) intersection.size() / union.size();
    }
    
    /**
     * Get information about the generic model being used
     */
    public java.util.Map<String, String> getModelInfo() {
        return java.util.Map.of(
            "model", "Simple character-based embedding",
            "note", "This is a demonstration. For production, use actual transformer models.",
            "recommendation", "Use sentence-transformers or OpenAI embeddings API"
        );
    }
}
