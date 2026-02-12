package org.example.ml.controller;

import org.example.ml.dto.SimilarityRequest;
import org.example.ml.service.EmbeddingService;
import org.example.ml.service.GenericEmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for embedding operations
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // For development - restrict in production
public class EmbeddingController {
    
    @Autowired
    private EmbeddingService embeddingService;
    
    @Autowired
    private GenericEmbeddingService huggingFaceService;
    
    /**
     * Train the custom embedding model
     */
    @PostMapping("/train")
    public ResponseEntity<Map<String, Object>> trainModel() {
        try {
            // Run training in a separate thread to avoid blocking
            new Thread(() -> {
                try {
                    embeddingService.trainModel();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "Training started");
            response.put("message", "Model training has been initiated. Check /api/status for progress.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleError("Failed to start training", e);
        }
    }
    
    /**
     * Get training status and progress
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("progress", embeddingService.getProgress());
            response.put("config", embeddingService.getModelConfig());
            response.put("isTrained", embeddingService.isModelTrained());
            
            if (embeddingService.isModelTrained()) {
                response.put("stats", embeddingService.getTrainingStats());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleError("Failed to get status", e);
        }
    }
    
    /**
     * Get current vocabulary
     */
    @GetMapping("/vocabulary")
    public ResponseEntity<Map<String, Object>> getVocabulary() {
        try {
            List<String> vocabulary = embeddingService.getVocabulary();
            
            Map<String, Object> response = new HashMap<>();
            response.put("vocabulary", vocabulary);
            response.put("size", vocabulary.size());
            response.put("isTrained", embeddingService.isModelTrained());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleError("Failed to get vocabulary", e);
        }
    }
    
    /**
     * Calculate similarity using custom model
     */
    @PostMapping("/similarity/custom")
    public ResponseEntity<Map<String, Object>> calculateCustomSimilarity(@RequestBody SimilarityRequest request) {
        try {
            float similarity = embeddingService.calculateCustomSimilarity(
                request.getWord1(), 
                request.getWord2()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("word1", request.getWord1());
            response.put("word2", request.getWord2());
            response.put("similarity", similarity);
            response.put("model", "custom");
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return handleError("Failed to calculate custom similarity", e);
        }
    }
    
    /**
     * Calculate similarity using generic HuggingFace model
     */
    @PostMapping("/similarity/generic")
    public ResponseEntity<Map<String, Object>> calculateGenericSimilarity(@RequestBody SimilarityRequest request) {
        try {
            float similarity = huggingFaceService.calculateGenericSimilarity(
                request.getWord1(), 
                request.getWord2()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("word1", request.getWord1());
            response.put("word2", request.getWord2());
            response.put("similarity", similarity);
            response.put("model", "generic");
            response.put("modelInfo", huggingFaceService.getModelInfo());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleError("Failed to calculate generic similarity", e);
        }
    }
    
    /**
     * Compare similarities from both models
     */
    @PostMapping("/similarity/compare")
    public ResponseEntity<Map<String, Object>> compareSimilarities(@RequestBody SimilarityRequest request) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("word1", request.getWord1());
            response.put("word2", request.getWord2());
            
            // Custom model similarity
            try {
                float customSim = embeddingService.calculateCustomSimilarity(
                    request.getWord1(), 
                    request.getWord2()
                );
                response.put("customSimilarity", customSim);
            } catch (Exception e) {
                response.put("customSimilarity", null);
                response.put("customError", e.getMessage());
            }
            
            // Generic model similarity
            try {
                float genericSim = huggingFaceService.calculateGenericSimilarity(
                    request.getWord1(), 
                    request.getWord2()
                );
                //genericSim=2f;
                response.put("genericSimilarity", genericSim);
            } catch (Exception e) {
                response.put("genericSimilarity", null);
                response.put("genericError", e.getMessage());
            }
            
            // Calculate difference if both succeeded
            if (response.containsKey("customSimilarity") && response.containsKey("genericSimilarity") &&
                response.get("customSimilarity") != null && response.get("genericSimilarity") != null) {
                float diff = (float) response.get("customSimilarity") - (float) response.get("genericSimilarity");
                response.put("difference", diff);
                response.put("customIsHigher", diff > 0);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleError("Failed to compare similarities", e);
        }
    }
    
    /**
     * Get embedding vector for a word
     */
    @GetMapping("/embedding/{word}")
    public ResponseEntity<Map<String, Object>> getEmbedding(@PathVariable String word) {
        try {
            float[] embedding = embeddingService.getEmbedding(word);
            
            Map<String, Object> response = new HashMap<>();
            response.put("word", word);
            response.put("embedding", embedding);
            response.put("dimension", embedding.length);
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return handleError("Failed to get embedding", e);
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("modelTrained", embeddingService.isModelTrained());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Helper method to handle errors
     */
    private ResponseEntity<Map<String, Object>> handleError(String message, Exception e) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("details", e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
