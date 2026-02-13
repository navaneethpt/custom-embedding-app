package org.search.embedding.controller;

import org.search.embedding.dto.SimilarityRequest;
import org.search.embedding.model.SiameseEmbedding;
import org.search.embedding.service.EmbeddingService;
import org.search.embedding.service.GenericEmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmbeddingController.class)
class EmbeddingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private GenericEmbeddingService genericEmbeddingService;

    @Test
    void testTrainModel_Success() throws Exception {
        // Setup
        doNothing().when(embeddingService).trainModel();

        // Execute & Verify
        mockMvc.perform(post("/api/train"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Training started"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testTrainModel_Exception() throws Exception {
        // Setup
        doThrow(new RuntimeException("Training failed")).when(embeddingService).trainModel();

        // Execute & Verify - Controller starts training in thread, exceptions are logged but controller returns success
        mockMvc.perform(post("/api/train"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Training started"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testGetStatus_Success() throws Exception {
        // Setup
        SiameseEmbedding.TrainingProgress progress = new SiameseEmbedding.TrainingProgress();
        progress.status = "Training completed";

        Map<String, Object> config = Map.of("embedDim", 16);

        when(embeddingService.getProgress()).thenReturn(progress);
        when(embeddingService.getModelConfig()).thenReturn(config);
        when(embeddingService.isModelTrained()).thenReturn(true);
        when(embeddingService.getTrainingStats()).thenReturn(Map.of("totalPairs", 10));

        // Execute & Verify
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.status").value("Training completed"))
                .andExpect(jsonPath("$.config.embedDim").value(16))
                .andExpect(jsonPath("$.isTrained").value(true))
                .andExpect(jsonPath("$.stats.totalPairs").value(10));
    }

    @Test
    void testGetStatus_Exception() throws Exception {
        // Setup
        when(embeddingService.getProgress()).thenThrow(new RuntimeException("Status error"));

        // Execute & Verify
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to get status"));
    }

    @Test
    void testGetVocabulary_Success() throws Exception {
        // Setup
        List<String> vocabulary = List.of("word1", "word2", "word3");
        when(embeddingService.getVocabulary()).thenReturn(vocabulary);
        when(embeddingService.isModelTrained()).thenReturn(true);

        // Execute & Verify
        mockMvc.perform(get("/api/vocabulary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vocabulary").isArray())
                .andExpect(jsonPath("$.vocabulary[0]").value("word1"))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.isTrained").value(true));
    }

    @Test
    void testCalculateCustomSimilarity_Success() throws Exception {
        // Setup
        SimilarityRequest request = new SimilarityRequest("word1", "word2");
        when(embeddingService.calculateCustomSimilarity("word1", "word2")).thenReturn(0.8f);

        // Execute & Verify
        mockMvc.perform(post("/api/similarity/custom")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"word1\":\"word1\",\"word2\":\"word2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word1").value("word1"))
                .andExpect(jsonPath("$.word2").value("word2"))
                .andExpect(jsonPath("$.similarity").value(0.8))
                .andExpect(jsonPath("$.model").value("custom"));
    }

    @Test
    void testCalculateCustomSimilarity_ModelNotTrained() throws Exception {
        // Setup
        when(embeddingService.calculateCustomSimilarity(anyString(), anyString()))
            .thenThrow(new IllegalStateException("Model not trained yet. Please train the model first."));

        // Execute & Verify
        mockMvc.perform(post("/api/similarity/custom")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"word1\":\"word1\",\"word2\":\"word2\"}"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.error").value("Model not trained yet. Please train the model first."));
    }

    @Test
    void testCalculateCustomSimilarity_WordNotInVocabulary() throws Exception {
        // Setup
        when(embeddingService.calculateCustomSimilarity(anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("Word not in vocabulary: unknown"));

        // Execute & Verify
        mockMvc.perform(post("/api/similarity/custom")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"word1\":\"word1\",\"word2\":\"unknown\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Word not in vocabulary: unknown"));
    }

    @Test
    void testCalculateGenericSimilarity_Success() throws Exception {
        // Setup
        when(genericEmbeddingService.calculateGenericSimilarity("word1", "word2")).thenReturn(0.7f);
        when(genericEmbeddingService.getModelInfo()).thenReturn(Map.of("model", "test"));

        // Execute & Verify
        mockMvc.perform(post("/api/similarity/generic")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"word1\":\"word1\",\"word2\":\"word2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word1").value("word1"))
                .andExpect(jsonPath("$.word2").value("word2"))
                .andExpect(jsonPath("$.similarity").value(0.7))
                .andExpect(jsonPath("$.model").value("generic"));
    }

    @Test
    void testCalculateGenericSimilarity_Exception() throws Exception {
        // Setup
        when(genericEmbeddingService.calculateGenericSimilarity(anyString(), anyString()))
            .thenThrow(new RuntimeException("Generic similarity failed"));

        // Execute & Verify
        mockMvc.perform(post("/api/similarity/generic")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"word1\":\"word1\",\"word2\":\"word2\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to calculate generic similarity"));
    }

    @Test
    void testCompareSimilarities_BothSuccess() throws Exception {
        // Setup
        when(embeddingService.calculateCustomSimilarity("word1", "word2")).thenReturn(0.8f);
        when(genericEmbeddingService.calculateGenericSimilarity("word1", "word2")).thenReturn(0.6f);
        when(genericEmbeddingService.getModelInfo()).thenReturn(Map.of("model", "test"));

        // Execute & Verify
        mockMvc.perform(post("/api/similarity/compare")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"word1\":\"word1\",\"word2\":\"word2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customSimilarity").value(0.8))
                .andExpect(jsonPath("$.genericSimilarity").value(0.6))
                .andExpect(jsonPath("$.difference").exists())
                .andExpect(jsonPath("$.customIsHigher").value(true));
    }

    @Test
    void testCompareSimilarities_CustomFails() throws Exception {
        // Setup
        when(embeddingService.calculateCustomSimilarity(anyString(), anyString()))
            .thenThrow(new IllegalStateException("Model not trained yet. Please train the model first."));
        when(genericEmbeddingService.calculateGenericSimilarity("word1", "word2")).thenReturn(0.6f);

        // Execute & Verify
        mockMvc.perform(post("/api/similarity/compare")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"word1\":\"word1\",\"word2\":\"word2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customSimilarity").isEmpty())
                .andExpect(jsonPath("$.customError").value("Model not trained yet. Please train the model first."))
                .andExpect(jsonPath("$.genericSimilarity").value(0.6));
    }

    @Test
    void testGetEmbedding_Success() throws Exception {
        // Setup
        float[] embedding = {0.1f, 0.2f, 0.3f};
        when(embeddingService.getEmbedding("word")).thenReturn(embedding);

        // Execute & Verify
        mockMvc.perform(get("/api/embedding/word"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word").value("word"))
                .andExpect(jsonPath("$.embedding").isArray())
                .andExpect(jsonPath("$.dimension").value(3));
    }

    @Test
    void testGetEmbedding_ModelNotTrained() throws Exception {
        // Setup
        when(embeddingService.getEmbedding(anyString()))
            .thenThrow(new IllegalStateException("Model not trained yet"));

        // Execute & Verify
        mockMvc.perform(get("/api/embedding/word"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.error").value("Model not trained yet"));
    }

    @Test
    void testHealth() throws Exception {
        // Setup
        when(embeddingService.isModelTrained()).thenReturn(true);

        // Execute & Verify
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.modelTrained").value(true));
    }
}