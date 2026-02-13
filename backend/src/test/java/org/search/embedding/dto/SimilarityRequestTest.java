package org.search.embedding.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimilarityRequestTest {

    @Test
    void testDefaultConstructor() {
        SimilarityRequest request = new SimilarityRequest();
        assertNull(request.getWord1());
        assertNull(request.getWord2());
    }

    @Test
    void testParameterizedConstructor() {
        SimilarityRequest request = new SimilarityRequest("hello", "world");
        assertEquals("hello", request.getWord1());
        assertEquals("world", request.getWord2());
    }

    @Test
    void testSettersAndGetters() {
        SimilarityRequest request = new SimilarityRequest();

        request.setWord1("test1");
        request.setWord2("test2");

        assertEquals("test1", request.getWord1());
        assertEquals("test2", request.getWord2());
    }

    @Test
    void testNullValues() {
        SimilarityRequest request = new SimilarityRequest();
        assertNull(request.getWord1());
        assertNull(request.getWord2());
    }
}