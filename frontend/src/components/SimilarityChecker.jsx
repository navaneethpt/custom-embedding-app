import React, { useState, useEffect } from 'react';
import apiService from '../services/apiService';
import './SimilarityChecker.css';

const SimilarityChecker = ({ modelTrained }) => {
  const [word1, setWord1] = useState('');
  const [word2, setWord2] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [vocabulary, setVocabulary] = useState([]);

  useEffect(() => {
    if (modelTrained) {
      fetchVocabulary();
    }
  }, [modelTrained]);

  const fetchVocabulary = async () => {
    try {
      const data = await apiService.getVocabulary();
      setVocabulary(data.vocabulary || []);
    } catch (err) {
      console.error('Error fetching vocabulary:', err);
    }
  };

  const handleCalculate = async () => {
    if (!word1 || !word2) {
      setError('Please enter both words');
      return;
    }

    if (!modelTrained) {
      setError('Please train the model first');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const data = await apiService.compareSimilarities(word1.toLowerCase(), word2.toLowerCase());
      setResult(data);
    } catch (err) {
      setError(err.response?.data?.error || err.message);
    } finally {
      setLoading(false);
    }
  };

  const getSimilarityColor = (similarity) => {
    if (similarity === null || similarity === undefined) return '#999';
    if (similarity > 0.7) return '#4caf50';
    if (similarity > 0.4) return '#ff9800';
    return '#f44336';
  };

  const getSimilarityLabel = (similarity) => {
    if (similarity === null || similarity === undefined) return 'N/A';
    if (similarity > 0.7) return 'High';
    if (similarity > 0.4) return 'Medium';
    return 'Low';
  };

  const getDifferenceIndicator = (diff) => {
    if (Math.abs(diff) < 0.1) return '≈ Similar';
    if (diff > 0) return '↑ Custom Higher';
    return '↓ Generic Higher';
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleCalculate();
    }
  };

  return (
    <div className="similarity-checker">
      <h2>🔍 Similarity Comparison</h2>
      
      {!modelTrained && (
        <div className="warning-message">
          ⚠️ Please train the model first before calculating similarities
        </div>
      )}

      <div className="input-section">
        <div className="input-group">
          <label htmlFor="word1">Word 1:</label>
          <input
            id="word1"
            type="text"
            value={word1}
            onChange={(e) => setWord1(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Enter first word"
            list="vocabulary1"
            disabled={!modelTrained}
          />
          <datalist id="vocabulary1">
            {vocabulary.map((word, idx) => (
              <option key={idx} value={word} />
            ))}
          </datalist>
        </div>

        <div className="vs-divider">vs</div>

        <div className="input-group">
          <label htmlFor="word2">Word 2:</label>
          <input
            id="word2"
            type="text"
            value={word2}
            onChange={(e) => setWord2(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Enter second word"
            list="vocabulary2"
            disabled={!modelTrained}
          />
          <datalist id="vocabulary2">
            {vocabulary.map((word, idx) => (
              <option key={idx} value={word} />
            ))}
          </datalist>
        </div>
      </div>

      <button 
        onClick={handleCalculate} 
        disabled={loading || !modelTrained}
        className="calculate-button"
      >
        {loading ? '⏳ Calculating...' : '🔍 Calculate Similarity'}
      </button>

      {error && (
        <div className="error-message">
          ⚠️ {error}
        </div>
      )}

      {result && (
        <div className="results-section">
          <h3>Results for "{result.word1}" vs "{result.word2}"</h3>
          
          <div className="comparison-grid">
            {/* Custom Model Result */}
            <div className="result-card custom">
              <div className="card-header">
                <h4>🎯 Custom Model</h4>
                <span className="model-badge">Domain-Specific</span>
              </div>
              
              {result.customError ? (
                <div className="result-error">
                  <p>❌ {result.customError}</p>
                </div>
              ) : (
                <>
                  <div 
                    className="similarity-score"
                    style={{ color: getSimilarityColor(result.customSimilarity) }}
                  >
                    {(result.customSimilarity * 100).toFixed(1)}%
                  </div>
                  <div className="similarity-label">
                    {getSimilarityLabel(result.customSimilarity)} Similarity
                  </div>
                  <div className="similarity-bar-container">
                    <div 
                      className="similarity-bar"
                      style={{ 
                        width: `${result.customSimilarity * 100}%`,
                        backgroundColor: getSimilarityColor(result.customSimilarity)
                      }}
                    />
                  </div>
                </>
              )}
            </div>

            {/* Generic Model Result */}
            <div className="result-card generic">
              <div className="card-header">
                <h4>🌐 Generic Model</h4>
                <span className="model-badge">General-Purpose</span>
              </div>
              
              {result.genericError ? (
                <div className="result-error">
                  <p>❌ {result.genericError}</p>
                </div>
              ) : (
                <>
                  <div 
                    className="similarity-score"
                    style={{ color: getSimilarityColor(result.genericSimilarity) }}
                  >
                    {(result.genericSimilarity * 100).toFixed(1)}%
                  </div>
                  <div className="similarity-label">
                    {getSimilarityLabel(result.genericSimilarity)} Similarity
                  </div>
                  <div className="similarity-bar-container">
                    <div 
                      className="similarity-bar"
                      style={{ 
                        width: `${result.genericSimilarity * 100}%`,
                        backgroundColor: getSimilarityColor(result.genericSimilarity)
                      }}
                    />
                  </div>
                </>
              )}
            </div>
          </div>

          {/* Difference Analysis */}
          {result.difference !== undefined && (
            <div className="difference-analysis">
              <h4>📊 Analysis</h4>
              <div className="difference-card">
                <div className="difference-value">
                  {getDifferenceIndicator(result.difference)}
                </div>
                <p className="difference-explanation">
                  {Math.abs(result.difference) < 0.1 
                    ? 'Both models show similar similarity scores for these words.'
                    : result.difference > 0
                      ? 'Your custom model finds these words more similar than the generic model, indicating domain-specific relationship.'
                      : 'The generic model finds these words more similar, possibly due to general language patterns.'}
                </p>
                <div className="difference-numeric">
                  Difference: {(Math.abs(result.difference) * 100).toFixed(1)}%
                </div>
              </div>
            </div>
          )}

          {/* Suggestions */}
          <div className="suggestions">
            <h4>💡 Try These Pairs:</h4>
            <div className="suggestion-tags">
              <button onClick={() => { setWord1('aws'); setWord2('s3'); }}>aws ↔ s3</button>
              <button onClick={() => { setWord1('azure'); setWord2('blob'); }}>azure ↔ blob</button>
              <button onClick={() => { setWord1('storage'); setWord2('compute'); }}>storage ↔ compute</button>
              <button onClick={() => { setWord1('kubernetes'); setWord2('container'); }}>kubernetes ↔ container</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SimilarityChecker;
