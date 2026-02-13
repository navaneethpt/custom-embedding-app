package org.search.embedding.service;

import org.search.embedding.model.SiameseEmbedding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocumentProcessorTest {

    private final DocumentProcessor processor = new DocumentProcessor();

    @TempDir
    Path tempDir;

    @Test
    void testProcessDocumentsFolder_ValidFolder() throws IOException {
        // Create test documents
        Path doc1 = tempDir.resolve("doc1.txt");
        Files.writeString(doc1, "The quick brown fox jumps over the lazy dog.");

        Path doc2 = tempDir.resolve("doc2.txt");
        Files.writeString(doc2, "Machine learning is a subset of artificial intelligence.");

        List<SiameseEmbedding.TrainingPair> pairs = processor.processDocumentsFolder(tempDir.toString());

        assertNotNull(pairs);
        assertTrue(pairs.size() > 0, "Should generate training pairs");
    }

    @Test
    void testProcessDocumentsFolder_InvalidFolder() {
        assertThrows(IOException.class, () -> {
            processor.processDocumentsFolder("/nonexistent/folder");
        });
    }

    @Test
    void testProcessDocumentsFolder_NoTxtFiles() throws IOException {
        Path nonTxtFile = tempDir.resolve("doc1.pdf");
        Files.writeString(nonTxtFile, "Some content");

        assertThrows(IOException.class, () -> {
            processor.processDocumentsFolder(tempDir.toString());
        });
    }

    @Test
    void testProcessDocument_ValidDocument() throws IOException {
        Path doc = tempDir.resolve("test.txt");
        Files.writeString(doc, "This is a test document. It contains multiple sentences.");

        List<SiameseEmbedding.TrainingPair> pairs = processor.processDocument(doc.toString());

        assertNotNull(pairs);
        assertTrue(pairs.size() > 0);
    }

    @Test
    void testProcessDocument_NonExistentFile() {
        assertThrows(IOException.class, () -> {
            processor.processDocument("/nonexistent/file.txt");
        });
    }

    @Test
    void testProcessSentence() throws Exception {
        // Access private method via reflection
        java.lang.reflect.Method method = DocumentProcessor.class.getDeclaredMethod("processSentence", String.class);
        method.setAccessible(true);

        String sentence = "The quick brown fox jumps";
        @SuppressWarnings("unchecked")
        List<SiameseEmbedding.TrainingPair> pairs = (List<SiameseEmbedding.TrainingPair>) method.invoke(processor, sentence);

        assertNotNull(pairs);
        // Should generate pairs for words within MAX_DISTANCE
    }

    @Test
    void testTokenize() throws Exception {
        java.lang.reflect.Method method = DocumentProcessor.class.getDeclaredMethod("tokenize", String.class);
        method.setAccessible(true);

        String text = "Hello, world! This is a TEST.";
        @SuppressWarnings("unchecked")
        List<String> tokens = (List<String>) method.invoke(processor, text);

        assertNotNull(tokens);
        assertTrue(tokens.contains("hello"));
        assertTrue(tokens.contains("world"));
        assertTrue(tokens.contains("test"));
        assertTrue(tokens.contains("this")); // not in stop words
        assertFalse(tokens.contains("is")); // stop word
        assertFalse(tokens.contains("a")); // stop word
    }

    @Test
    void testGetProcessingStats() {
        List<SiameseEmbedding.TrainingPair> pairs = List.of(
            new SiameseEmbedding.TrainingPair("word1", "word2", 1.0f),
            new SiameseEmbedding.TrainingPair("word2", "word3", 0.5f),
            new SiameseEmbedding.TrainingPair("word1", "word3", 0.3f)
        );

        Map<String, Object> stats = processor.getProcessingStats(pairs);

        assertNotNull(stats);
        assertEquals(3, stats.get("totalPairs"));
        assertEquals(3, stats.get("uniqueWords"));
        assertTrue((Float) stats.get("avgSimilarity") > 0);
        assertTrue((Integer) ((List<?>) stats.get("topWords")).size() <= 10);
    }

    @Test
    void testCreateSamplePairs() {
        List<SiameseEmbedding.TrainingPair> pairs = processor.createSamplePairs();

        assertNotNull(pairs);
        assertTrue(pairs.size() > 0);
        assertTrue(pairs.get(0).similarity >= 0.0f && pairs.get(0).similarity <= 1.0f);
    }
}