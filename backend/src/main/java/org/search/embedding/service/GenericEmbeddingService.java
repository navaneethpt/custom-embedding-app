package org.search.embedding.service;

import ai.djl.Model;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;


@Service
public class GenericEmbeddingService {

    private static final String MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2";

    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;

    @PostConstruct
    public void init() throws Exception {
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                // Use the DJL model zoo URL (PyTorch)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/" + MODEL_NAME)
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .optEngine("PyTorch") // use the PyTorch engine
                .build();

        model = criteria.loadModel();
        predictor = model.newPredictor();
    }

    public float calculateGenericSimilarity(String text1, String text2) {
        try {
            float[] emb1 = predictor.predict(text1);
            float[] emb2 = predictor.predict(text2);
            return cosineSimilarity(emb1, emb2);
        } catch (TranslateException e) {
            throw new RuntimeException("Failed to compute embeddings", e);
        }
    }

    private float cosineSimilarity(float[] v1, float[] v2) {
        if (v1.length != v2.length) throw new IllegalArgumentException("Vector length mismatch");

        float dot = 0f, n1 = 0f, n2 = 0f;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            n1 += v1[i] * v1[i];
            n2 += v2[i] * v2[i];
        }
        double denom = Math.sqrt(n1) * Math.sqrt(n2);
        return denom == 0 ? 0f : (float) (dot / denom);
    }

    @PreDestroy
    public void destroy() {
        if (predictor != null) predictor.close();
        if (model != null) model.close();
    }
    public java.util.Map<String, String> getModelInfo() {
        return java.util.Map.of(
                "model", "Simple character-based embedding",
                "note", "This is a demonstration. For production, use actual transformer models.",
                "recommendation", "Use sentence-transformers or OpenAI embeddings API"
        );
    }
}
