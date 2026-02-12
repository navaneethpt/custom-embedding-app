package org.example.ml.service;

import jakarta.annotation.PreDestroy;
import org.example.ml.model.SiameseEmbedding;
import org.example.ml.model.SiameseEmbedding.TrainingPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for managing custom embedding model training and inference
 */
@Service
public class EmbeddingService {
    
    @Autowired
    private DocumentProcessor documentProcessor;
    
    @Value("${embedding.dimension:16}")
    private int embedDim;
    
    @Value("${embedding.margin:2.0}")
    private float margin;
    
    @Value("${embedding.epochs:300}")
    private int epochs;
    
    @Value("${embedding.learning-rate:0.01}")
    private float learningRate;
    
    @Value("${documents.folder:src/main/resources/documents}")
    private String documentsFolder;
    
    private SiameseEmbedding model;
    private List<TrainingPair> trainingPairs;
    
    /**
     * Train the embedding model on documents in the configured folder
     */
    public synchronized void trainModel() throws Exception {
        if (model != null && model.isTrained()) {
            System.out.println("Model already trained. Creating new instance...");
            model.close();
        }
        
        // Process documents
        System.out.println("Processing documents from: " + documentsFolder);
        try {
            trainingPairs = documentProcessor.processDocumentsFolder(documentsFolder);
        } catch (Exception e) {
            System.err.println("Error processing documents: " + e.getMessage());
            System.out.println("Using sample training pairs instead...");
            trainingPairs = documentProcessor.createSamplePairs();
        }
        
        if (trainingPairs.isEmpty()) {
            throw new IllegalStateException("No training pairs generated");
        }
        
        // Create and train model
        model = new SiameseEmbedding(embedDim, margin, epochs, learningRate);
        model.train(trainingPairs);
        
        System.out.println("Model training completed successfully");
    }
    
    /**
     * Calculate similarity between two words using custom model
     */
    public float calculateCustomSimilarity(String word1, String word2) throws Exception {
        if (model == null || !model.isTrained()) {
            throw new IllegalStateException("Model not trained yet. Please train the model first.");
        }
        
        if (!model.hasWord(word1)) {
            throw new IllegalArgumentException("Word not in vocabulary: " + word1);
        }
        
        if (!model.hasWord(word2)) {
            throw new IllegalArgumentException("Word not in vocabulary: " + word2);
        }
        
        return model.calculateSimilarity(word1, word2);
    }
    
    /**
     * Get embedding vector for a word
     */
    public float[] getEmbedding(String word) throws Exception {
        if (model == null || !model.isTrained()) {
            throw new IllegalStateException("Model not trained yet");
        }
        
        if (!model.hasWord(word)) {
            throw new IllegalArgumentException("Word not in vocabulary: " + word);
        }
        
        return model.getEmbedding(word);
    }
    
    /**
     * Get current vocabulary
     */
    public List<String> getVocabulary() {
        if (model == null) {
            return List.of();
        }
        return model.getVocabulary();
    }
    
    /**
     * Get training progress
     */
    public SiameseEmbedding.TrainingProgress getProgress() {
        if (model == null) {
            SiameseEmbedding.TrainingProgress progress = new SiameseEmbedding.TrainingProgress();
            progress.status = "Model not initialized";
            return progress;
        }
        return model.getProgress();
    }
    
    /**
     * Check if model is trained
     */
    public boolean isModelTrained() {
        return model != null && model.isTrained();
    }
    
    /**
     * Get training statistics
     */
    public Map<String, Object> getTrainingStats() {
        if (trainingPairs == null) {
            return Map.of("error", "No training data available");
        }
        return documentProcessor.getProcessingStats(trainingPairs);
    }
    
    /**
     * Get model configuration
     */
    public Map<String, Object> getModelConfig() {
        return Map.of(
            "embedDim", embedDim,
            "margin", margin,
            "epochs", epochs,
            "learningRate", learningRate,
            "documentsFolder", documentsFolder,
            "isTrained", isModelTrained(),
            "vocabularySize", model != null ? model.getVocabularySize() : 0
        );
    }
    
    /**
     * Save the trained model
     */
    public void saveModel(String path) throws Exception {
        if (model == null || !model.isTrained()) {
            throw new IllegalStateException("Model not trained yet");
        }
        model.saveModel(path);
    }
    
    /**
     * Cleanup resources on shutdown
     */
    @PreDestroy
    public void cleanup() {
        if (model != null) {
            model.close();
        }
    }
}
