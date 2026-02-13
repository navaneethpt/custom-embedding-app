package org.search.embedding.service;

import org.search.embedding.model.SiameseEmbedding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private DocumentProcessor documentProcessor;

    @Mock
    private SiameseEmbedding siameseEmbedding;

    @Spy
    @InjectMocks
    private EmbeddingService embeddingService;

    @Test
    void testTrainModel_Success() throws Exception {
        // Setup
        List<SiameseEmbedding.TrainingPair> mockPairs = List.of(
            new SiameseEmbedding.TrainingPair("word1", "word2", 1.0f)
        );

        ReflectionTestUtils.setField(embeddingService, "documentsFolder", "/test/docs");
        when(documentProcessor.processDocumentsFolder("/test/docs")).thenReturn(mockPairs);
        doReturn(siameseEmbedding).when(embeddingService).createModel(anyInt(), anyFloat(), anyInt(), anyFloat());
        when(siameseEmbedding.isTrained()).thenReturn(false, true); // Initially false, then true after training

        // Execute
        embeddingService.trainModel();

        // Verify
        verify(documentProcessor).processDocumentsFolder("/test/docs");
        verify(embeddingService).createModel(anyInt(), anyFloat(), anyInt(), anyFloat());
        verify(siameseEmbedding).train(mockPairs);
        assertTrue(embeddingService.isModelTrained());
    }

    @Test
    void testTrainModel_DocumentProcessingFails() throws Exception {
        // Setup
        when(documentProcessor.processDocumentsFolder(anyString()))
            .thenThrow(new RuntimeException("Processing failed"));
        when(documentProcessor.createSamplePairs()).thenReturn(List.of());

        // Execute
        assertThrows(IllegalStateException.class, () -> embeddingService.trainModel());
    }

    @Test
    void testTrainModel_RetrainExistingModel() throws Exception {
        // Setup
        List<SiameseEmbedding.TrainingPair> mockPairs = List.of(
            new SiameseEmbedding.TrainingPair("word1", "word2", 1.0f)
        );

        SiameseEmbedding oldModel = mock(SiameseEmbedding.class);
        ReflectionTestUtils.setField(embeddingService, "model", oldModel);
        ReflectionTestUtils.setField(embeddingService, "documentsFolder", "/test/docs");
        when(oldModel.isTrained()).thenReturn(true);

        when(documentProcessor.processDocumentsFolder("/test/docs")).thenReturn(mockPairs);
        doReturn(siameseEmbedding).when(embeddingService).createModel(anyInt(), anyFloat(), anyInt(), anyFloat());

        // Execute
        embeddingService.trainModel();

        // Verify old model was closed
        verify(oldModel).close();
        verify(embeddingService).createModel(anyInt(), anyFloat(), anyInt(), anyFloat());
        verify(siameseEmbedding).train(mockPairs);
    }

    @Test
    void testCalculateCustomSimilarity_Success() throws Exception {
        // Setup
        ReflectionTestUtils.setField(embeddingService, "model", siameseEmbedding);
        when(siameseEmbedding.isTrained()).thenReturn(true);
        when(siameseEmbedding.hasWord("word1")).thenReturn(true);
        when(siameseEmbedding.hasWord("word2")).thenReturn(true);
        when(siameseEmbedding.calculateSimilarity("word1", "word2")).thenReturn(0.8f);

        // Execute
        float result = embeddingService.calculateCustomSimilarity("word1", "word2");

        // Verify
        assertEquals(0.8f, result);
        verify(siameseEmbedding).calculateSimilarity("word1", "word2");
    }

    @Test
    void testCalculateCustomSimilarity_ModelNotTrained() {
        // Execute & Verify
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            embeddingService.calculateCustomSimilarity("word1", "word2"));
        assertTrue(exception.getMessage().contains("not trained"));
    }

    @Test
    void testCalculateCustomSimilarity_WordNotInVocabulary() {
        // Setup
        ReflectionTestUtils.setField(embeddingService, "model", siameseEmbedding);
        when(siameseEmbedding.isTrained()).thenReturn(true);
        when(siameseEmbedding.hasWord("word1")).thenReturn(true);
        when(siameseEmbedding.hasWord("unknown")).thenReturn(false);

        // Execute & Verify
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            embeddingService.calculateCustomSimilarity("word1", "unknown"));
        assertTrue(exception.getMessage().contains("not in vocabulary"));
    }

    @Test
    void testGetEmbedding_Success() throws Exception {
        // Setup
        ReflectionTestUtils.setField(embeddingService, "model", siameseEmbedding);
        when(siameseEmbedding.isTrained()).thenReturn(true);
        when(siameseEmbedding.hasWord("word")).thenReturn(true);
        float[] mockEmbedding = {0.1f, 0.2f, 0.3f};
        when(siameseEmbedding.getEmbedding("word")).thenReturn(mockEmbedding);

        // Execute
        float[] result = embeddingService.getEmbedding("word");

        // Verify
        assertArrayEquals(mockEmbedding, result);
    }

    @Test
    void testGetVocabulary_NoModel() {
        List<String> vocab = embeddingService.getVocabulary();
        assertNotNull(vocab);
        assertTrue(vocab.isEmpty());
    }

    @Test
    void testGetVocabulary_WithModel() {
        // Setup
        ReflectionTestUtils.setField(embeddingService, "model", siameseEmbedding);
        List<String> mockVocab = List.of("word1", "word2");
        when(siameseEmbedding.getVocabulary()).thenReturn(mockVocab);

        // Execute
        List<String> result = embeddingService.getVocabulary();

        // Verify
        assertEquals(mockVocab, result);
    }

    @Test
    void testGetProgress_NoModel() {
        // Ensure model field is null for this test
        ReflectionTestUtils.setField(embeddingService, "model", null);
        SiameseEmbedding.TrainingProgress progress = embeddingService.getProgress();
        assertNotNull(progress);
        assertEquals("Model not initialized", progress.status);
    }

    @Test
    void testGetProgress_WithModel() {
        // Setup
        ReflectionTestUtils.setField(embeddingService, "model", siameseEmbedding);
        SiameseEmbedding.TrainingProgress mockProgress = new SiameseEmbedding.TrainingProgress();
        mockProgress.status = "Training";
        when(siameseEmbedding.getProgress()).thenReturn(mockProgress);

        // Execute
        SiameseEmbedding.TrainingProgress result = embeddingService.getProgress();

        // Verify
        assertEquals("Training", result.status);
    }

    @Test
    void testIsModelTrained_NoModel() {
        assertFalse(embeddingService.isModelTrained());
    }

    @Test
    void testIsModelTrained_WithModel() {
        ReflectionTestUtils.setField(embeddingService, "model", siameseEmbedding);
        when(siameseEmbedding.isTrained()).thenReturn(true);
        assertTrue(embeddingService.isModelTrained());
    }

    @Test
    void testGetTrainingStats_NoTrainingPairs() {
        Map<String, Object> stats = embeddingService.getTrainingStats();
        assertNotNull(stats);
        assertEquals("No training data available", stats.get("error"));
    }

    @Test
    void testGetTrainingStats_WithTrainingPairs() {
        // Setup
        List<SiameseEmbedding.TrainingPair> mockPairs = List.of(
            new SiameseEmbedding.TrainingPair("word1", "word2", 1.0f)
        );
        ReflectionTestUtils.setField(embeddingService, "trainingPairs", mockPairs);

        Map<String, Object> mockStats = Map.of("totalPairs", 1, "uniqueWords", 2);
        when(documentProcessor.getProcessingStats(mockPairs)).thenReturn(mockStats);

        // Execute
        Map<String, Object> result = embeddingService.getTrainingStats();

        // Verify
        assertEquals(mockStats, result);
    }

    @Test
    void testGetModelConfig() {
        // Setup
        ReflectionTestUtils.setField(embeddingService, "embedDim", 16);
        ReflectionTestUtils.setField(embeddingService, "margin", 2.0f);
        ReflectionTestUtils.setField(embeddingService, "epochs", 100);
        ReflectionTestUtils.setField(embeddingService, "learningRate", 0.01f);
        ReflectionTestUtils.setField(embeddingService, "documentsFolder", "/test/docs");

        // Execute
        Map<String, Object> config = embeddingService.getModelConfig();

        // Verify
        assertEquals(16, config.get("embedDim"));
        assertEquals(2.0f, config.get("margin"));
        assertEquals(100, config.get("epochs"));
        assertEquals(0.01f, config.get("learningRate"));
        assertEquals("/test/docs", config.get("documentsFolder"));
        assertEquals(false, config.get("isTrained"));
        assertEquals(0, config.get("vocabularySize"));
    }

    @Test
    void testSaveModel_Success() throws Exception {
        // Setup
        ReflectionTestUtils.setField(embeddingService, "model", siameseEmbedding);
        when(siameseEmbedding.isTrained()).thenReturn(true);

        // Execute
        embeddingService.saveModel("/tmp/test.model");

        // Verify
        verify(siameseEmbedding).saveModel("/tmp/test.model");
    }

    @Test
    void testSaveModel_ModelNotTrained() {
        assertThrows(IllegalStateException.class, () ->
            embeddingService.saveModel("/tmp/test.model"));
    }

    @Test
    void testCleanup() {
        // Setup
        ReflectionTestUtils.setField(embeddingService, "model", siameseEmbedding);

        // Execute
        embeddingService.cleanup();

        // Verify
        verify(siameseEmbedding).close();
    }
}