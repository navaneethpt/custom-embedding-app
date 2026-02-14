# Custom Domain-Specific Embedding System

A full-stack application demonstrating custom embedding generation using Siamese neural networks for domain-specific semantic search. This project implements the concepts from [this blog post](https://navaneethpt.tech/posts/custom_embedding/).

## 🎯 Overview

This application allows you to:
- Train custom word embeddings on domain-specific documents
- Compare similarity between words using your custom-trained model
- Compare results against generic HuggingFace embeddings
- Visualize the effectiveness of domain-specific embeddings

## 🏗️ Architecture

```
┌─────────────────┐
│   React UI      │
│  (Frontend)     │
└────────┬────────┘
         │ REST API
┌────────▼────────┐
│  Spring Boot    │
│    Backend      │
├─────────────────┤
│  DJL Engine     │
│ (Siamese NN)    │
└─────────────────┘
```

### Key Components

1. **Backend (Java + DJL)**
   - Siamese neural network for custom embeddings
   - Document processing and training data generation
   - REST API endpoints
   - Integration with HuggingFace models for comparison

2. **Frontend (React)**
   - Document upload interface
   - Training control panel
   - Similarity comparison tool
   - Results visualization

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Node.js 16 or higher
- Maven 3.6+
- npm or yarn

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/custom-embedding-app.git
   cd custom-embedding-app
   ```

2. **Add your documents**
   ```bash
   # Place your .txt documents in the documents folder
   cp your-documents/*.txt backend/src/main/resources/documents/
   ```

3. **Start the backend**
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```
   Backend will start on `http://localhost:8080`

4. **Start the frontend**
   ```bash
   cd frontend
   npm install
   npm start
   ```
   Frontend will start on `http://localhost:3000`

## 📖 How It Works

### 1. Training Data Generation

The system automatically:
- Reads documents from the `documents` folder
- Tokenizes content into sentences
- Builds word pair correlations based on proximity
- Generates similarity labels (1.0 for adjacent words, decreasing with distance)

Example:
```
Sentence: "AWS S3 object storage"
Pairs: {aws,s3,1.0}, {aws,object,0.5}, {s3,object,1.0}, {s3,storage,0.5}
```

### 2. Siamese Network Architecture

```
Input (one-hot) → Linear(vocab→32) → ReLU → Linear(32→embedDim) → Embedding
```

Both words in a pair pass through the **same network** (weight sharing).

### 3. Contrastive Loss

```
loss = y × dist² + (1-y) × max(margin - dist, 0)²
```

Where:
- `y`: similarity label (0-1)
- `dist`: Euclidean distance between embeddings
- `margin`: minimum distance for dissimilar pairs (default: 2.0)

### 4. Embedding & Similarity

Once trained, words are embedded using the network:
```java
NDArray embedding = predictor.predict(oneHot(word));
similarity = cosineSimilarity(embed1, embed2);
```

## 🎮 Usage

### Training the Model

1. Navigate to `http://localhost:3000`
2. Click **"Train Model"** button
3. Wait for training to complete (progress shown in UI)
4. Vocabulary and training stats will be displayed

### Comparing Similarities

1. Enter two words from your domain vocabulary
2. Click **"Calculate Similarity"**
3. View results:
   - **Custom Model**: Your domain-specific similarity
   - **Generic Model**: HuggingFace embedding similarity
   - **Difference**: How much your custom model differs

### Example Use Case

**Documents**: Cloud computing documentation (AWS, Azure, GCP)

**Query**: "s3" vs "blob-storage"

**Results**:
- Generic embedding: Low similarity (different terms)
- Custom embedding: High similarity (both are object storage services)

## 📊 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/train` | Train the embedding model |
| GET | `/api/status` | Get training status |
| POST | `/api/similarity/custom` | Calculate custom similarity |
| POST | `/api/similarity/generic` | Calculate generic similarity |
| POST | `/api/similarity/compare` | Compare both similarities |
| GET | `/api/vocabulary` | Get current vocabulary |

## 🔧 Configuration

### Backend (`application.properties`)

```properties
# DJL Configuration
embedding.dimension=16
embedding.margin=2.0
embedding.epochs=300
embedding.learning-rate=0.01

# Document Processing
documents.folder=src/main/resources/documents
documents.max-distance=5
```

### Frontend (`.env`)

```env
REACT_APP_API_URL=http://localhost:8080/api
```

## 📁 Project Structure

```
custom-embedding-app/
├── backend/
│   ├── src/main/java/org/example/ml/
│   │   ├── model/
│   │   │   └── SiameseEmbedding.java
│   │   ├── service/
│   │   │   ├── EmbeddingService.java
│   │   │   ├── DocumentProcessor.java
│   │   │   └── HuggingFaceService.java
│   │   ├── controller/
│   │   │   └── EmbeddingController.java
│   │   └── dto/
│   │       └── SimilarityRequest.java
│   ├── src/main/resources/
│   │   └── documents/          # Place your .txt files here
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── TrainingPanel.jsx
│   │   │   ├── SimilarityChecker.jsx
│   │   │   └── ResultsDisplay.jsx
│   │   ├── services/
│   │   │   └── apiService.js
│   │   └── App.js
│   └── package.json
└── README.md
```

## 🧪 Testing

```bash
# Backend tests
cd backend
mvn test

# Frontend tests
cd frontend
npm test
```

## 🔬 Technical Details

### Why Custom Embeddings?

Generic embeddings (Word2Vec, BERT, etc.) are trained on general corpora and may not capture domain-specific relationships:

**Problem**: Generic models don't know that "EC2" and "virtual-machine" are highly related in cloud computing context.

**Solution**: Train embeddings on your specific domain where proximity in text indicates semantic relationship.

### Siamese Network Benefits

1. **Weight Sharing**: Same network for all inputs → learns consistent representation
2. **Contrastive Learning**: Explicitly learns what's similar vs dissimilar
3. **Flexible**: Works with small datasets (100s of pairs)
4. **Domain-Adaptive**: Captures your specific similarity logic

### Comparison with Generic Models

| Aspect | Custom Embedding | Generic Embedding |
|--------|-----------------|-------------------|
| Training Data | Your documents | Internet-scale corpus |
| Vocabulary | Domain-specific | General language |
| Similarity Logic | Your definitions | Statistical co-occurrence |
| Size | Small (KB) | Large (GB) |
| Inference | Fast | Slower |

## 🚧 Limitations

- Requires labeled similarity data (auto-generated from proximity)
- Limited to vocabulary seen during training
- Quality depends on document quality and diversity
- Not suitable for general-purpose language tasks

## 🛠️ Extending the Project

### Add More Features

1. **Batch Processing**: Process multiple document sets
2. **Model Versioning**: Save/load different trained models
3. **Visualization**: t-SNE plots of embedding space
4. **API Authentication**: Secure the endpoints
5. **Vector Database**: Integrate with Pinecone/Weaviate for large-scale search

### Improve Training

1. **Data Augmentation**: Synonym replacement, paraphrasing
2. **Triplet Loss**: Use triplet loss instead of contrastive
3. **Hard Negative Mining**: Focus on difficult examples
4. **Cross-Validation**: Validate on held-out pairs

## 📚 References

- [Original Blog Post](https://navaneethpt.tech/posts/custom_embedding/)
- [DJL Documentation](https://djl.ai/)
- [Siamese Networks](https://www.cs.cmu.edu/~rsalakhu/papers/oneshot1.pdf)
- [Contrastive Learning](https://arxiv.org/abs/2002.05709)

## 📝 License

MIT License - feel free to use this project for learning and commercial purposes.

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## 👤 Author

Navaneeth P T - [https://github.com/navaneethpt](https://github.com/yourusername)

## 🙏 Acknowledgments

- DJL (Deep Java Library) team
- HuggingFace for pre-trained models
