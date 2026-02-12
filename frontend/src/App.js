import React, { useState, useEffect } from 'react';
import TrainingPanel from './components/TrainingPanel';
import SimilarityChecker from './components/SimilarityChecker';
import apiService from './services/apiService';
import './App.css';

function App() {
  const [modelTrained, setModelTrained] = useState(false);
  const [systemStatus, setSystemStatus] = useState('Checking...');

  useEffect(() => {
    checkHealth();
  }, []);

  const checkHealth = async () => {
    try {
      const health = await apiService.healthCheck();
      setModelTrained(health.modelTrained);
      setSystemStatus(health.status === 'healthy' ? '✓ Connected' : '⚠ Disconnected');
    } catch (err) {
      setSystemStatus('⚠ Backend Offline');
      console.error('Health check failed:', err);
    }
  };

  const handleTrainingComplete = () => {
    setModelTrained(true);
  };

  return (
    <div className="App">
      <header className="app-header">
        <div className="header-content">
          <h1>🎯 Custom Domain-Specific Embeddings</h1>
          <p className="subtitle">
            Train Siamese neural networks on your documents for domain-aware semantic search
          </p>
          <div className="system-status">
            <span className="status-indicator">{systemStatus}</span>
          </div>
        </div>
      </header>

      <main className="app-main">
        <div className="container">
          {/* Information Banner */}
          <div className="info-banner">
            <h3>📚 How It Works</h3>
            <ol>
              <li>
                <strong>Add Documents:</strong> Place your .txt files in 
                <code>backend/src/main/resources/documents/</code>
              </li>
              <li>
                <strong>Train Model:</strong> Click "Train Model" to learn domain-specific embeddings
              </li>
              <li>
                <strong>Compare Similarities:</strong> Test word similarities using both custom and generic models
              </li>
            </ol>
            <p className="info-note">
              💡 <strong>Why Custom Embeddings?</strong> Generic models don't capture domain-specific 
              relationships. For example, "EC2" and "virtual-machine" may seem unrelated to a generic 
              model, but are highly related in cloud computing context.
            </p>
          </div>

          {/* Training Section */}
          <section className="section">
            <TrainingPanel onTrainingComplete={handleTrainingComplete} />
          </section>

          {/* Similarity Checker Section */}
          <section className="section">
            <SimilarityChecker modelTrained={modelTrained} />
          </section>

          {/* Technical Details */}
          <section className="section technical-details">
            <h2>🔬 Technical Details</h2>
            <div className="details-grid">
              <div className="detail-card">
                <h4>Architecture</h4>
                <p>Siamese Neural Network with shared weights</p>
                <code>Input → Linear(vocab→32) → ReLU → Linear(32→16) → Embedding</code>
              </div>
              
              <div className="detail-card">
                <h4>Loss Function</h4>
                <p>Contrastive Loss</p>
                <code>loss = y × dist² + (1-y) × max(margin - dist, 0)²</code>
              </div>
              
              <div className="detail-card">
                <h4>Training Data</h4>
                <p>Auto-generated from word proximity</p>
                <code>similarity = 1.0 / distance_between_words</code>
              </div>
              
              <div className="detail-card">
                <h4>Framework</h4>
                <p>DJL (Deep Java Library) with PyTorch engine</p>
                <code>Java 17 + Spring Boot + React</code>
              </div>
            </div>
          </section>

          {/* Footer */}
          <footer className="app-footer">
            <p>
              Based on the blog post: 
              <a 
                href="https://navaneethpt.tech/posts/custom_embedding/" 
                target="_blank" 
                rel="noopener noreferrer"
              >
                Why Generic Embeddings Fail for Domain-Specific Search
              </a>
            </p>
            <p>
              Built with ❤️ using Java DJL, Spring Boot, and React
            </p>
          </footer>
        </div>
      </main>
    </div>
  );
}

export default App;
