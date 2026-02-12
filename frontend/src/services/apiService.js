import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const apiService = {
  // Training endpoints
  trainModel: async () => {
    const response = await apiClient.post('/train');
    return response.data;
  },

  getStatus: async () => {
    const response = await apiClient.get('/status');
    return response.data;
  },

  getVocabulary: async () => {
    const response = await apiClient.get('/vocabulary');
    return response.data;
  },

  // Similarity endpoints
  calculateCustomSimilarity: async (word1, word2) => {
    const response = await apiClient.post('/similarity/custom', { word1, word2 });
    return response.data;
  },

  calculateGenericSimilarity: async (word1, word2) => {
    const response = await apiClient.post('/similarity/generic', { word1, word2 });
    return response.data;
  },

  compareSimilarities: async (word1, word2) => {
    const response = await apiClient.post('/similarity/compare', { word1, word2 });
    return response.data;
  },

  getEmbedding: async (word) => {
    const response = await apiClient.get(`/embedding/${word}`);
    return response.data;
  },

  // Health check
  healthCheck: async () => {
    const response = await apiClient.get('/health');
    return response.data;
  },
};

export default apiService;
