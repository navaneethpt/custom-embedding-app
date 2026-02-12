package org.example.ml.model;

import ai.djl.Model;
import ai.djl.engine.Engine;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.djl.nn.Block;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.GradientCollector;
import ai.djl.training.Trainer;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.Tracker;
import ai.djl.translate.NoopTranslator;
import ai.djl.inference.Predictor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Siamese Neural Network for Custom Domain-Specific Embeddings
 * Based on contrastive learning approach for semantic similarity
 */
public class SiameseEmbedding {

    private final Map<String, Integer> wordToIdx = new ConcurrentHashMap<>();
    private final List<String> vocabulary = new ArrayList<>();
    private int vocabSize = 0;
    
    private final int embedDim;
    private final float margin;
    private final int epochs;
    private final float learningRate;
    
    private Model model;
    private Trainer trainer;
    private NDManager manager;
    private Predictor<NDList, NDList> predictor;
    
    private boolean isTrained = false;
    private TrainingProgress progress;
    
    public static class TrainingPair {
        public final String word1;
        public final String word2;
        public final float similarity;
        
        public TrainingPair(String word1, String word2, float similarity) {
            this.word1 = word1;
            this.word2 = word2;
            this.similarity = similarity;
        }
        
        @Override
        public String toString() {
            return String.format("{%s, %s, %.2f}", word1, word2, similarity);
        }
    }
    
    public static class TrainingProgress {
        public int currentEpoch = 0;
        public int totalEpochs = 0;
        public float currentLoss = 0f;
        public boolean isTraining = false;
        public String status = "Not started";
        public List<Float> lossHistory = new ArrayList<>();
    }
    
    /**
     * Custom Contrastive Loss Implementation
     * loss = y × dist² + (1-y) × max(margin - dist, 0)²
     */
    private static class ContrastiveLoss extends Loss {
        private final float margin;
        private final float eps = 1e-12f;

        public ContrastiveLoss(float margin) {
            super("contrastiveLoss");
            this.margin = margin;
        }

        @Override
        public NDArray evaluate(NDList labels, NDList predictions) {
            NDArray y = labels.singletonOrThrow();
            NDArray e1 = predictions.get(0);
            NDArray e2 = predictions.get(1);

            // Ensure y shape (batch, 1)
            if (y.getShape().dimension() == 1) {
                y = y.reshape(y.getShape().get(0), 1);
            }

            // Normalize embeddings
            e1 = l2Normalize(e1);
            e2 = l2Normalize(e2);

            // Euclidean distance
            NDArray diff = e1.sub(e2);
            NDArray dist = diff.mul(diff).sum(new int[]{1}, true).add(eps).sqrt();

            // Contrastive loss components
            NDArray lossSimilar = y.mul(dist.mul(dist));
            NDArray md = dist.mul(-1).add(margin);
            NDArray clamp = md.maximum(0f);
            NDArray lossDissim = y.mul(-1).add(1f).mul(clamp.mul(clamp));

            return lossSimilar.add(lossDissim).mean();
        }

        private NDArray l2Normalize(NDArray x) {
            NDArray norm = x.mul(x).sum(new int[]{1}, true).add(eps).sqrt();
            return x.div(norm);
        }
    }
    
    public SiameseEmbedding(int embedDim, float margin, int epochs, float learningRate) {
        this.embedDim = embedDim;
        this.margin = margin;
        this.epochs = epochs;
        this.learningRate = learningRate;
        this.progress = new TrainingProgress();
        this.progress.totalEpochs = epochs;
    }
    
    /**
     * Build vocabulary from training pairs
     */
    public void buildVocabulary(List<TrainingPair> pairs) {
        Set<String> uniqueWords = new HashSet<>();
        for (TrainingPair pair : pairs) {
            uniqueWords.add(pair.word1.toLowerCase());
            uniqueWords.add(pair.word2.toLowerCase());
        }
        
        vocabulary.clear();
        vocabulary.addAll(uniqueWords);
        Collections.sort(vocabulary);
        
        wordToIdx.clear();
        for (int i = 0; i < vocabulary.size(); i++) {
            wordToIdx.put(vocabulary.get(i), i);
        }
        
        vocabSize = vocabulary.size();
        System.out.println("Vocabulary built: " + vocabSize + " unique words");
    }
    
    /**
     * Create one-hot encoding for a word
     */
    private NDArray oneHot(String word) {
        Integer idx = wordToIdx.get(word.toLowerCase());
        if (idx == null) {
            throw new IllegalArgumentException("Word not in vocabulary: " + word);
        }
        NDArray vec = manager.zeros(new Shape(1, vocabSize));
        vec.set(new ai.djl.ndarray.index.NDIndex(0, idx), 1f);
        return vec;
    }
    
    /**
     * Create the embedding network architecture
     */
    private Block createEmbeddingNet() {
        return new SequentialBlock()
                .add(Linear.builder().setUnits(32).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(embedDim).build());
    }
    
    /**
     * Train the Siamese network on word pairs
     */
    public void train(List<TrainingPair> pairs) throws Exception {
        System.out.println("Starting training with " + pairs.size() + " pairs...");
        System.out.println("DJL Engine: " + Engine.getInstance().getEngineName());
        
        progress.isTraining = true;
        progress.status = "Building vocabulary...";
        buildVocabulary(pairs);
        
        progress.status = "Initializing model...";
        manager = NDManager.newBaseManager();
        model = Model.newInstance("siamese-embedding");
        model.setBlock(createEmbeddingNet());
        
        Optimizer optimizer = Adam.builder()
                .optLearningRateTracker(Tracker.fixed(learningRate))
                .build();
        
        Loss contrastiveLoss = new ContrastiveLoss(margin);
        
        DefaultTrainingConfig config = new DefaultTrainingConfig(contrastiveLoss)
                .optOptimizer(optimizer);
        
        trainer = model.newTrainer(config);
        trainer.initialize(new Shape(1, vocabSize));
        
        progress.status = "Training...";
        progress.lossHistory.clear();
        
        for (int epoch = 0; epoch < epochs; epoch++) {
            float totalLoss = 0f;
            
            for (TrainingPair pair : pairs) {
                try {
                    NDArray x1 = oneHot(pair.word1);
                    NDArray x2 = oneHot(pair.word2);
                    NDArray y = manager.create(new float[]{pair.similarity}).reshape(1, 1);
                    
                    NDArray e1;
                    NDArray e2;
                    
                    try (GradientCollector gc = trainer.newGradientCollector()) {
                        e1 = trainer.forward(new NDList(x1)).singletonOrThrow();
                        e2 = trainer.forward(new NDList(x2)).singletonOrThrow();
                        
                        NDArray lossVal = contrastiveLoss.evaluate(new NDList(y), new NDList(e1, e2));
                        totalLoss += lossVal.toFloatArray()[0];
                        
                        gc.backward(lossVal);
                    }
                    
                    trainer.step();
                } catch (Exception e) {
                    System.err.println("Error processing pair: " + pair + " - " + e.getMessage());
                }
            }
            
            progress.currentEpoch = epoch + 1;
            progress.currentLoss = totalLoss;
            progress.lossHistory.add(totalLoss);
            
            if (epoch % 50 == 0 || epoch == epochs - 1) {
                System.out.printf("Epoch %d/%d, Loss: %.4f%n", epoch + 1, epochs, totalLoss);
            }
        }
        
        // Initialize predictor for inference
        predictor = model.newPredictor(new NoopTranslator());
        isTrained = true;
        progress.isTraining = false;
        progress.status = "Training completed";
        
        System.out.println("Training completed successfully!");
    }
    
    /**
     * Get embedding for a single word
     */
    public float[] getEmbedding(String word) throws Exception {
        if (!isTrained) {
            throw new IllegalStateException("Model not trained yet");
        }
        
        NDArray x = oneHot(word);
        NDArray embedding = predictor.predict(new NDList(x)).singletonOrThrow();
        return embedding.toFloatArray();
    }
    
    /**
     * Calculate cosine similarity between two words
     */
    public float calculateSimilarity(String word1, String word2) throws Exception {
        if (!isTrained) {
            throw new IllegalStateException("Model not trained yet");
        }
        
        NDArray x1 = oneHot(word1);
        NDArray x2 = oneHot(word2);
        
        NDArray e1 = predictor.predict(new NDList(x1)).singletonOrThrow();
        NDArray e2 = predictor.predict(new NDList(x2)).singletonOrThrow();
        
        return cosineSimilarity(e1, e2);
    }
    
    /**
     * Calculate cosine similarity between two embedding vectors
     */
    private float cosineSimilarity(NDArray a, NDArray b) {
        float eps = 1e-12f;
        NDArray an = a.div(a.mul(a).sum().sqrt().add(eps));
        NDArray bn = b.div(b.mul(b).sum().sqrt().add(eps));
        return an.mul(bn).sum().toFloatArray()[0];
    }
    
    /**
     * Check if a word exists in the vocabulary
     */
    public boolean hasWord(String word) {
        return wordToIdx.containsKey(word.toLowerCase());
    }
    
    /**
     * Get the current vocabulary
     */
    public List<String> getVocabulary() {
        return new ArrayList<>(vocabulary);
    }
    
    /**
     * Get vocabulary size
     */
    public int getVocabularySize() {
        return vocabSize;
    }
    
    /**
     * Get training progress
     */
    public TrainingProgress getProgress() {
        return progress;
    }
    
    /**
     * Check if model is trained
     */
    public boolean isTrained() {
        return isTrained;
    }
    
    /**
     * Close and cleanup resources
     */
    public void close() {
        if (predictor != null) {
            predictor.close();
        }
        if (trainer != null) {
            trainer.close();
        }
        if (model != null) {
            model.close();
        }
        if (manager != null) {
            manager.close();
        }
    }
    
    /**
     * Save model to file
     */
    public void saveModel(String path) throws Exception {
        if (!isTrained) {
            throw new IllegalStateException("Model not trained yet");
        }
        model.save(java.nio.file.Paths.get(path), "siamese-embedding");
        System.out.println("Model saved to: " + path);
    }
}
