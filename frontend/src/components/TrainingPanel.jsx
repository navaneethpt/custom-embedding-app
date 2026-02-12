import React, { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import apiService from '../services/apiService';
import './TrainingPanel.css';

const TrainingPanel = ({ onTrainingComplete }) => {
  const [status, setStatus] = useState(null);
  const [isTraining, setIsTraining] = useState(false);
  const [vocabulary, setVocabulary] = useState([]);
  const [stats, setStats] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, 2000); // Poll every 2 seconds
    return () => clearInterval(interval);
  }, []);

  const fetchStatus = async () => {
    try {
      const data = await apiService.getStatus();
      setStatus(data);
      setIsTraining(data.progress?.isTraining || false);
      
      if (data.isTrained && data.stats) {
        setStats(data.stats);
      }
      
      if (data.isTrained) {
        fetchVocabulary();
        if (onTrainingComplete) {
          onTrainingComplete();
        }
      }
    } catch (err) {
      console.error('Error fetching status:', err);
    }
  };

  const fetchVocabulary = async () => {
    try {
      const data = await apiService.getVocabulary();
      setVocabulary(data.vocabulary || []);
    } catch (err) {
      console.error('Error fetching vocabulary:', err);
    }
  };

  const handleTrain = async () => {
    try {
      setError(null);
      await apiService.trainModel();
      setIsTraining(true);
    } catch (err) {
      setError('Failed to start training: ' + err.message);
    }
  };

  const getLossChartData = () => {
    if (!status?.progress?.lossHistory) return [];
    return status.progress.lossHistory.map((loss, index) => ({
      epoch: index + 1,
      loss: loss,
    }));
  };

  const getProgressPercentage = () => {
    if (!status?.progress) return 0;
    return Math.round((status.progress.currentEpoch / status.progress.totalEpochs) * 100);
  };

  return (
    <div className="training-panel">
      <h2>🧠 Model Training</h2>
      
      {/* Training Control */}
      <div className="training-control">
        <button 
          onClick={handleTrain} 
          disabled={isTraining}
          className="train-button"
        >
          {isTraining ? '⏳ Training...' : '🚀 Train Model'}
        </button>
        
        {status?.isTrained && !isTraining && (
          <span className="status-badge success">✓ Model Trained</span>
        )}
      </div>

      {error && (
        <div className="error-message">
          ⚠️ {error}
        </div>
      )}

      {/* Training Progress */}
      {isTraining && status?.progress && (
        <div className="training-progress">
          <h3>Training Progress</h3>
          <div className="progress-bar-container">
            <div 
              className="progress-bar" 
              style={{ width: `${getProgressPercentage()}%` }}
            />
          </div>
          <p>
            Epoch {status.progress.currentEpoch} / {status.progress.totalEpochs} 
            ({getProgressPercentage()}%)
          </p>
          <p className="loss-value">Current Loss: {status.progress.currentLoss.toFixed(4)}</p>
          <p className="status-text">{status.progress.status}</p>
        </div>
      )}

      {/* Loss Chart */}
      {status?.progress?.lossHistory && status.progress.lossHistory.length > 0 && (
        <div className="loss-chart">
          <h3>Training Loss</h3>
          <ResponsiveContainer width="100%" height={250}>
            <LineChart data={getLossChartData()}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="epoch" label={{ value: 'Epoch', position: 'insideBottom', offset: -5 }} />
              <YAxis label={{ value: 'Loss', angle: -90, position: 'insideLeft' }} />
              <Tooltip />
              <Legend />
              <Line 
                type="monotone" 
                dataKey="loss" 
                stroke="#8884d8" 
                strokeWidth={2}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Model Configuration */}
      {status?.config && (
        <div className="model-config">
          <h3>Model Configuration</h3>
          <div className="config-grid">
            <div className="config-item">
              <span className="config-label">Embedding Dimension:</span>
              <span className="config-value">{status.config.embedDim}</span>
            </div>
            <div className="config-item">
              <span className="config-label">Margin:</span>
              <span className="config-value">{status.config.margin}</span>
            </div>
            <div className="config-item">
              <span className="config-label">Epochs:</span>
              <span className="config-value">{status.config.epochs}</span>
            </div>
            <div className="config-item">
              <span className="config-label">Learning Rate:</span>
              <span className="config-value">{status.config.learningRate}</span>
            </div>
            <div className="config-item">
              <span className="config-label">Vocabulary Size:</span>
              <span className="config-value">{status.config.vocabularySize}</span>
            </div>
          </div>
        </div>
      )}

      {/* Training Statistics */}
      {stats && (
        <div className="training-stats">
          <h3>Training Statistics</h3>
          <div className="stats-grid">
            <div className="stat-item">
              <span className="stat-label">Total Pairs:</span>
              <span className="stat-value">{stats.totalPairs}</span>
            </div>
            <div className="stat-item">
              <span className="stat-label">Unique Words:</span>
              <span className="stat-value">{stats.uniqueWords}</span>
            </div>
            <div className="stat-item">
              <span className="stat-label">Avg Similarity:</span>
              <span className="stat-value">{stats.avgSimilarity?.toFixed(3)}</span>
            </div>
          </div>
          
          {stats.topWords && stats.topWords.length > 0 && (
            <div className="top-words">
              <h4>Most Frequent Words:</h4>
              <div className="word-tags">
                {stats.topWords.map((word, idx) => (
                  <span key={idx} className="word-tag">{word}</span>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Vocabulary Display */}
      {vocabulary.length > 0 && (
        <div className="vocabulary-section">
          <h3>Vocabulary ({vocabulary.length} words)</h3>
          <div className="vocabulary-list">
            {vocabulary.slice(0, 50).map((word, idx) => (
              <span key={idx} className="vocab-word">{word}</span>
            ))}
            {vocabulary.length > 50 && (
              <span className="vocab-more">... and {vocabulary.length - 50} more</span>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default TrainingPanel;
