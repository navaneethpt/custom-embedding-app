# Project Structure

```
custom-embedding-app/
│
├── README.md                          # Main project documentation
├── SETUP.md                           # Detailed setup instructions
├── LICENSE                            # MIT License
├── .gitignore                         # Git ignore rules
│
├── backend/                           # Java Spring Boot backend
│   ├── pom.xml                        # Maven dependencies
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/org/example/ml/
│   │   │   │   ├── CustomEmbeddingApplication.java    # Main Spring Boot app
│   │   │   │   ├── model/
│   │   │   │   │   └── SiameseEmbedding.java          # Neural network implementation
│   │   │   │   ├── service/
│   │   │   │   │   ├── EmbeddingService.java          # Model training & inference
│   │   │   │   │   ├── DocumentProcessor.java         # Document parsing & pair generation
│   │   │   │   │   └── HuggingFaceService.java        # Generic embedding service
│   │   │   │   ├── controller/
│   │   │   │   │   └── EmbeddingController.java       # REST API endpoints
│   │   │   │   └── dto/
│   │   │   │       └── SimilarityRequest.java         # Request DTOs
│   │   │   └── resources/
│   │   │       ├── application.properties             # Backend configuration
│   │   │       └── documents/                         # 📁 PLACE YOUR .TXT FILES HERE
│   │   │           ├── cloud_computing.txt
│   │   │           └── databases.txt
│   │   └── test/                                      # Unit tests (optional)
│   └── target/                                        # Compiled classes (generated)
│
└── frontend/                          # React frontend
    ├── package.json                   # Node dependencies
    ├── public/
    │   └── index.html                 # HTML template
    └── src/
        ├── index.js                   # React entry point
        ├── index.css                  # Global styles
        ├── App.js                     # Main application component
        ├── App.css                    # App styles
        ├── services/
        │   └── apiService.js          # API client
        └── components/
            ├── TrainingPanel.jsx      # Model training UI
            ├── TrainingPanel.css      # Training panel styles
            ├── SimilarityChecker.jsx  # Similarity comparison UI
            └── SimilarityChecker.css  # Similarity checker styles
```

## Key Components

### Backend Architecture

#### 1. Model Layer (`model/`)
- **SiameseEmbedding.java**: Core neural network implementation
  - Siamese network architecture
  - Contrastive loss function
  - Training loop
  - Inference predictor

#### 2. Service Layer (`service/`)
- **EmbeddingService.java**: Orchestrates model operations
  - Training coordination
  - Similarity calculations
  - Model state management
  
- **DocumentProcessor.java**: Processes documents into training data
  - Text parsing and tokenization
  - Word pair generation based on proximity
  - Similarity label assignment
  
- **HuggingFaceService.java**: Generic embeddings for comparison
  - Character-based embedding (demo)
  - Baseline similarity calculation

#### 3. Controller Layer (`controller/`)
- **EmbeddingController.java**: REST API endpoints
  - `/api/train` - Start model training
  - `/api/status` - Get training progress
  - `/api/vocabulary` - Get learned vocabulary
  - `/api/similarity/*` - Calculate similarities

#### 4. Data Layer (`dto/`)
- **SimilarityRequest.java**: Request/response objects

### Frontend Architecture

#### 1. Core Application
- **App.js**: Main application container
  - State management
  - Component orchestration
  - System status monitoring

#### 2. Components
- **TrainingPanel**: Model training interface
  - Training controls
  - Progress visualization
  - Loss chart (Recharts)
  - Vocabulary display
  
- **SimilarityChecker**: Similarity comparison tool
  - Word input with autocomplete
  - Side-by-side comparison
  - Visual similarity indicators
  - Difference analysis

#### 3. Services
- **apiService.js**: Centralized API communication
  - Axios-based HTTP client
  - Endpoint abstractions
  - Error handling

## Data Flow

### Training Flow
```
1. User clicks "Train Model"
   ↓
2. Frontend → POST /api/train
   ↓
3. Backend starts training thread
   ↓
4. DocumentProcessor reads .txt files
   ↓
5. Generates word pairs with similarity labels
   ↓
6. SiameseEmbedding trains neural network
   ↓
7. Frontend polls GET /api/status for progress
   ↓
8. Training completes, vocabulary available
```

### Similarity Calculation Flow
```
1. User enters two words
   ↓
2. Frontend → POST /api/similarity/compare
   ↓
3. EmbeddingService:
   - Calculates custom embedding similarity
   - Calls HuggingFaceService for generic similarity
   ↓
4. Returns both similarities + difference
   ↓
5. Frontend displays visual comparison
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/train` | Initiate model training |
| GET | `/api/status` | Get training status & progress |
| GET | `/api/vocabulary` | Get learned vocabulary |
| POST | `/api/similarity/custom` | Calculate custom similarity |
| POST | `/api/similarity/generic` | Calculate generic similarity |
| POST | `/api/similarity/compare` | Compare both similarities |
| GET | `/api/embedding/{word}` | Get embedding vector |
| GET | `/api/health` | Health check |

## Technology Stack

### Backend
- **Java 17**: Programming language
- **Spring Boot 3.2**: Web framework
- **DJL 0.26.0**: Deep learning framework
- **PyTorch**: Neural network engine
- **Maven**: Build tool

### Frontend
- **React 18**: UI framework
- **Axios**: HTTP client
- **Recharts**: Data visualization
- **CSS3**: Styling

## Configuration Files

### Backend Configuration
`backend/src/main/resources/application.properties`
```properties
embedding.dimension=16           # Embedding vector size
embedding.margin=2.0            # Contrastive loss margin
embedding.epochs=300            # Training iterations
embedding.learning-rate=0.01    # Optimizer learning rate
documents.folder=...            # Documents directory
```

### Frontend Configuration
`frontend/.env` (optional)
```env
REACT_APP_API_URL=http://localhost:8080/api
```

## File Locations

### Adding Documents
Place your .txt files here:
```
backend/src/main/resources/documents/
```

### Generated Artifacts
- Backend build: `backend/target/`
- Frontend build: `frontend/build/`
- DJL cache: `.djl.ai/` (auto-generated)

## Development Workflow

1. **Add Documents**: Place .txt files in documents folder
2. **Start Backend**: `mvn spring-boot:run`
3. **Start Frontend**: `npm start`
4. **Train Model**: Click "Train Model" button
5. **Test Similarities**: Enter word pairs
6. **Iterate**: Add more documents, retrain, compare

## Extension Points

### Adding Features

1. **New Endpoints**: Add to `EmbeddingController.java`
2. **New Services**: Create in `service/` package
3. **UI Components**: Add to `frontend/src/components/`
4. **Styles**: Create corresponding `.css` files

### Customization

- **Loss Function**: Modify `ContrastiveLoss` in `SiameseEmbedding.java`
- **Network Architecture**: Update `createEmbeddingNet()` method
- **Document Processing**: Adjust `DocumentProcessor.java` logic
- **UI Theme**: Edit CSS variables in `App.css`

## Best Practices

1. **Documents**: Use domain-specific text (5-10 files minimum)
2. **Training**: Start with fewer epochs (100) for testing
3. **Vocabulary**: Words must appear in training documents
4. **Performance**: Larger embedding dimensions = better quality but slower
5. **Testing**: Compare custom vs generic for validation
