package org.example.ml.service;

import org.example.ml.model.SiameseEmbedding.TrainingPair;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Document processor that generates training pairs from text documents
 * using word proximity as a similarity signal
 */
@Service
public class DocumentProcessor {
    
    private static final int MAX_DISTANCE = 5; // Maximum word distance to consider
    private static final String[] STOP_WORDS = {
        "a", "an", "the", "is", "are", "was", "were", "in", "on", "at", 
        "to", "for", "of", "and", "or", "but", "with", "from", "by"
    };
    private static final Set<String> STOP_WORDS_SET = new HashSet<>(Arrays.asList(STOP_WORDS));
    
    /**
     * Process all documents in a folder and generate training pairs
     */
    public List<TrainingPair> processDocumentsFolder(String folderPath) throws IOException {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IOException("Invalid documents folder: " + folderPath);
        }
        
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        if (files == null || files.length == 0) {
            throw new IOException("No .txt files found in: " + folderPath);
        }
        
        System.out.println("Processing " + files.length + " documents...");
        
        List<TrainingPair> allPairs = new ArrayList<>();
        for (File file : files) {
            System.out.println("Processing: " + file.getName());
            List<TrainingPair> filePairs = processDocument(file.getAbsolutePath());
            allPairs.addAll(filePairs);
        }
        
        // Deduplicate pairs
        Map<String, TrainingPair> uniquePairs = new HashMap<>();
        for (TrainingPair pair : allPairs) {
            String key = getKey(pair.word1, pair.word2);
            if (!uniquePairs.containsKey(key) || uniquePairs.get(key).similarity < pair.similarity) {
                uniquePairs.put(key, pair);
            }
        }
        
        List<TrainingPair> result = new ArrayList<>(uniquePairs.values());
        System.out.println("Generated " + result.size() + " unique training pairs");
        return result;
    }
    
    /**
     * Process a single document and generate training pairs
     */
    public List<TrainingPair> processDocument(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        String content = Files.readString(path);
        
        // Split into sentences
        String[] sentences = content.split("[.!?]+");
        
        List<TrainingPair> pairs = new ArrayList<>();
        
        for (String sentence : sentences) {
            List<TrainingPair> sentencePairs = processSentence(sentence);
            pairs.addAll(sentencePairs);
        }
        
        return pairs;
    }
    
    /**
     * Process a sentence and generate word pairs based on proximity
     */
    private List<TrainingPair> processSentence(String sentence) {
        List<TrainingPair> pairs = new ArrayList<>();
        
        // Tokenize and clean
        List<String> words = tokenize(sentence);
        if (words.size() < 2) {
            return pairs;
        }
        
        // Generate pairs based on distance
        for (int i = 0; i < words.size(); i++) {
            for (int j = i + 1; j < words.size() && j <= i + MAX_DISTANCE; j++) {
                String word1 = words.get(i);
                String word2 = words.get(j);
                
                // Calculate similarity based on inverse distance
                int distance = j - i;
                float similarity = 1.0f / distance;
                
                pairs.add(new TrainingPair(word1, word2, similarity));
            }
        }
        
        return pairs;
    }
    
    /**
     * Tokenize sentence into words
     */
    private List<String> tokenize(String sentence) {
        // Remove punctuation and convert to lowercase
        String cleaned = sentence.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        
        if (cleaned.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Split into words and filter
        return Arrays.stream(cleaned.split("\\s+"))
                .filter(word -> word.length() > 2) // Minimum word length
                .filter(word -> !STOP_WORDS_SET.contains(word)) // Remove stop words
                .filter(word -> !word.matches("\\d+")) // Remove pure numbers
                .collect(Collectors.toList());
    }
    
    /**
     * Create a unique key for a word pair (order-independent)
     */
    private String getKey(String word1, String word2) {
        if (word1.compareTo(word2) < 0) {
            return word1 + "|" + word2;
        } else {
            return word2 + "|" + word1;
        }
    }
    
    /**
     * Get statistics about processed documents
     */
    public Map<String, Object> getProcessingStats(List<TrainingPair> pairs) {
        Map<String, Object> stats = new HashMap<>();
        
        Set<String> uniqueWords = new HashSet<>();
        Map<String, Integer> wordFrequency = new HashMap<>();
        
        float avgSimilarity = 0f;
        float maxSimilarity = 0f;
        float minSimilarity = 1f;
        
        for (TrainingPair pair : pairs) {
            uniqueWords.add(pair.word1);
            uniqueWords.add(pair.word2);
            
            wordFrequency.put(pair.word1, wordFrequency.getOrDefault(pair.word1, 0) + 1);
            wordFrequency.put(pair.word2, wordFrequency.getOrDefault(pair.word2, 0) + 1);
            
            avgSimilarity += pair.similarity;
            maxSimilarity = Math.max(maxSimilarity, pair.similarity);
            minSimilarity = Math.min(minSimilarity, pair.similarity);
        }
        
        avgSimilarity /= pairs.size();
        
        // Get top 10 most frequent words
        List<Map.Entry<String, Integer>> topWords = wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());
        
        stats.put("totalPairs", pairs.size());
        stats.put("uniqueWords", uniqueWords.size());
        stats.put("avgSimilarity", avgSimilarity);
        stats.put("maxSimilarity", maxSimilarity);
        stats.put("minSimilarity", minSimilarity);
        stats.put("topWords", topWords.stream()
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.toList()));
        
        return stats;
    }
    
    /**
     * Create sample training pairs for testing (when no documents available)
     */
    public List<TrainingPair> createSamplePairs() {
        return Arrays.asList(
            new TrainingPair("aws", "s3", 1.0f),
            new TrainingPair("aws", "ec2", 1.0f),
            new TrainingPair("s3", "storage", 1.0f),
            new TrainingPair("s3", "bucket", 1.0f),
            new TrainingPair("ec2", "instance", 1.0f),
            new TrainingPair("ec2", "virtual", 0.5f),
            new TrainingPair("azure", "blob", 1.0f),
            new TrainingPair("azure", "storage", 0.5f),
            new TrainingPair("gcp", "bucket", 0.5f),
            new TrainingPair("aws", "azure", 0.3f),
            new TrainingPair("aws", "database", 0.1f),
            new TrainingPair("storage", "compute", 0.0f)
        );
    }
}
