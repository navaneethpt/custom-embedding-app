package org.search.embedding.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HuggingFaceServiceTest {

    private final HuggingFaceService service = new HuggingFaceService();

    @Test
    void testCalculateGenericSimilarity_IdenticalWords() {
        float similarity = service.calculateGenericSimilarity("test", "test");
        assertTrue(similarity >= 0.5f, "Identical words should have high similarity");
    }

    @Test
    void testCalculateGenericSimilarity_DifferentWords() {
        float similarity = service.calculateGenericSimilarity("hello", "world");
        assertTrue(similarity >= 0.0f && similarity <= 1.0f, "Similarity should be between 0 and 1");
    }

    @Test
    void testCalculateGenericSimilarity_SimilarWords() {
        float similarity = service.calculateGenericSimilarity("hello", "hello");
        float similarity2 = service.calculateGenericSimilarity("hello", "world");
        assertTrue(similarity >= similarity2, "Identical words should be more similar than different words");
    }

    @Test
    void testGetSimpleEmbedding_NonNull() {
        // Testing private method via reflection for completeness
        try {
            java.lang.reflect.Method method = HuggingFaceService.class.getDeclaredMethod("getSimpleEmbedding", String.class);
            method.setAccessible(true);
            float[] embedding = (float[]) method.invoke(service, "test");
            assertNotNull(embedding);
            assertTrue(embedding.length > 0);
        } catch (Exception e) {
            fail("Reflection test failed: " + e.getMessage());
        }
    }

    @Test
    void testCosineSimilarity_IdenticalVectors() {
        try {
            java.lang.reflect.Method method = HuggingFaceService.class.getDeclaredMethod("cosineSimilarity", float[].class, float[].class);
            method.setAccessible(true);
            float[] vec = {1.0f, 2.0f, 3.0f};
            float similarity = (float) method.invoke(service, vec, vec);
            assertEquals(1.0f, similarity, 0.001f, "Identical vectors should have similarity 1");
        } catch (Exception e) {
            fail("Reflection test failed: " + e.getMessage());
        }
    }

    @Test
    void testCosineSimilarity_OrthogonalVectors() {
        try {
            java.lang.reflect.Method method = HuggingFaceService.class.getDeclaredMethod("cosineSimilarity", float[].class, float[].class);
            method.setAccessible(true);
            float[] vec1 = {1.0f, 0.0f};
            float[] vec2 = {0.0f, 1.0f};
            float similarity = (float) method.invoke(service, vec1, vec2);
            assertEquals(0.0f, similarity, 0.001f, "Orthogonal vectors should have similarity 0");
        } catch (Exception e) {
            fail("Reflection test failed: " + e.getMessage());
        }
    }

    @Test
    void testSimpleStringSimilarity_IdenticalStrings() {
        try {
            java.lang.reflect.Method method = HuggingFaceService.class.getDeclaredMethod("simpleStringSimilarity", String.class, String.class);
            method.setAccessible(true);
            float similarity = (float) method.invoke(service, "test", "test");
            assertEquals(1.0f, similarity, 0.001f, "Identical strings should have similarity 1");
        } catch (Exception e) {
            fail("Reflection test failed: " + e.getMessage());
        }
    }

    @Test
    void testSimpleStringSimilarity_DifferentStrings() {
        try {
            java.lang.reflect.Method method = HuggingFaceService.class.getDeclaredMethod("simpleStringSimilarity", String.class, String.class);
            method.setAccessible(true);
            float similarity = (float) method.invoke(service, "abc", "def");
            assertTrue(similarity >= 0.0f && similarity <= 1.0f);
        } catch (Exception e) {
            fail("Reflection test failed: " + e.getMessage());
        }
    }

    @Test
    void testGetModelInfo() {
        var modelInfo = service.getModelInfo();
        assertNotNull(modelInfo);
        assertTrue(modelInfo.containsKey("model"));
        assertTrue(modelInfo.containsKey("note"));
        assertTrue(modelInfo.containsKey("recommendation"));
    }
}