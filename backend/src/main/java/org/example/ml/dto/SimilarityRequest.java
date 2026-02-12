package org.example.ml.dto;

/**
 * Request DTO for similarity calculation
 */
public class SimilarityRequest {
    private String word1;
    private String word2;
    
    public SimilarityRequest() {}
    
    public SimilarityRequest(String word1, String word2) {
        this.word1 = word1;
        this.word2 = word2;
    }
    
    public String getWord1() {
        return word1;
    }
    
    public void setWord1(String word1) {
        this.word1 = word1;
    }
    
    public String getWord2() {
        return word2;
    }
    
    public void setWord2(String word2) {
        this.word2 = word2;
    }
}
