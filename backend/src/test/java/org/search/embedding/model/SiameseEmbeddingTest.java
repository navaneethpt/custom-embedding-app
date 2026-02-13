package org.search.embedding.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SiameseEmbeddingTest {

    @Test
    void testConstructor() {
        SiameseEmbedding model = new SiameseEmbedding(16, 2.0f, 100, 0.01f);
        assertNotNull(model);
        assertFalse(model.isTrained());
    }

    @Test
    void testBuildVocabulary() {
        SiameseEmbedding model = new SiameseEmbedding(16, 2.0f, 100, 0.01f);

        List<SiameseEmbedding.TrainingPair> pairs = List.of(
            new SiameseEmbedding.TrainingPair("hello", "world", 1.0f),
            new SiameseEmbedding.TrainingPair("foo", "bar", 0.5f),
            new SiameseEmbedding.TrainingPair("hello", "foo", 0.3f)
        );

        model.buildVocabulary(pairs);

        assertTrue(model.hasWord("hello"));
        assertTrue(model.hasWord("world"));
        assertTrue(model.hasWord("foo"));
        assertTrue(model.hasWord("bar"));
        assertFalse(model.hasWord("nonexistent"));

        assertEquals(4, model.getVocabularySize());
        List<String> vocab = model.getVocabulary();
        assertEquals(4, vocab.size());
        assertTrue(vocab.contains("hello"));
        assertTrue(vocab.contains("world"));
    }

    @Test
    void testBuildVocabulary_CaseInsensitive() {
        SiameseEmbedding model = new SiameseEmbedding(16, 2.0f, 100, 0.01f);

        List<SiameseEmbedding.TrainingPair> pairs = List.of(
            new SiameseEmbedding.TrainingPair("Hello", "WORLD", 1.0f)
        );

        model.buildVocabulary(pairs);

        assertTrue(model.hasWord("hello"));
        assertTrue(model.hasWord("Hello"));
        assertTrue(model.hasWord("world"));
        assertTrue(model.hasWord("WORLD"));
    }

    @Test
    void testGetVocabulary_UntrainedModel() {
        SiameseEmbedding model = new SiameseEmbedding(16, 2.0f, 100, 0.01f);
        List<String> vocab = model.getVocabulary();
        assertNotNull(vocab);
        assertTrue(vocab.isEmpty());
    }

    @Test
    void testGetVocabularySize_UntrainedModel() {
        SiameseEmbedding model = new SiameseEmbedding(16, 2.0f, 100, 0.01f);
        assertEquals(0, model.getVocabularySize());
    }

    @Test
    void testHasWord_UntrainedModel() {
        SiameseEmbedding model = new SiameseEmbedding(16, 2.0f, 100, 0.01f);
        assertFalse(model.hasWord("anyword"));
    }

    @Test
    void testGetProgress_UntrainedModel() {
        SiameseEmbedding model = new SiameseEmbedding(16, 2.0f, 100, 0.01f);
        SiameseEmbedding.TrainingProgress progress = model.getProgress();

        assertNotNull(progress);
        assertEquals(0, progress.currentEpoch);
        assertEquals(100, progress.totalEpochs);
        assertEquals(0.0f, progress.currentLoss);
        assertFalse(progress.isTraining);
        assertEquals("Not started", progress.status);
        assertNotNull(progress.lossHistory);
        assertTrue(progress.lossHistory.isEmpty());
    }

    @Test
    void testIsTrained_UntrainedModel() {
        SiameseEmbedding model = new SiameseEmbedding(16, 2.0f, 100, 0.01f);
        assertFalse(model.isTrained());
    }

    @Test
    void testTrainingPairConstructor() {
        SiameseEmbedding.TrainingPair pair = new SiameseEmbedding.TrainingPair("word1", "word2", 0.8f);
        assertEquals("word1", pair.word1);
        assertEquals("word2", pair.word2);
        assertEquals(0.8f, pair.similarity);
    }

    @Test
    void testTrainingPairToString() {
        SiameseEmbedding.TrainingPair pair = new SiameseEmbedding.TrainingPair("word1", "word2", 0.8f);
        String expected = "{word1, word2, 0.80}";
        assertEquals(expected, pair.toString());
    }

    @Test
    void testGetEmbedding_UntrainedModel() {
        SiameseEmbedding model = new SiameseEmbedding(16, 2.0f, 100, 0.01f);
        assertThrows(IllegalStateException.class, () -> {
            model.getEmbedding("word");
        });
    }

    @Test
    void testCalculateSimilarity_UntrainedModel() {
        SiameseEmbedding model = new SiameseEmbedding(16, 2.0f, 100, 0.01f);
        assertThrows(IllegalStateException.class, () -> {
            model.calculateSimilarity("word1", "word2");
        });
    }

    @Test
    void testSaveModel_UntrainedModel() {
        SiameseEmbedding model = new SiameseEmbedding(16, 2.0f, 100, 0.01f);
        assertThrows(IllegalStateException.class, () -> {
            model.saveModel("/tmp/test.model");
        });
    }

    @Test
    void testClose_UntrainedModel() {
        SiameseEmbedding model = new SiameseEmbedding(16, 2.0f, 100, 0.01f);
        // Should not throw exception
        assertDoesNotThrow(() -> model.close());
    }
}