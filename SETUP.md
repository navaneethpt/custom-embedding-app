# Setup Instructions

This guide will help you set up and run the Custom Domain-Specific Embedding System.

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17 or higher** - [Download](https://adoptium.net/)
  ```bash
  java -version
  ```

- **Maven 3.6+** - [Download](https://maven.apache.org/download.cgi)
  ```bash
  mvn -version
  ```

- **Node.js 16+ and npm** - [Download](https://nodejs.org/)
  ```bash
  node -version
  npm -version
  ```

## Step 1: Clone or Download the Repository

```bash
# If using Git
git clone <repository-url>
cd custom-embedding-app

# Or simply navigate to the downloaded folder
cd custom-embedding-app
```

## Step 2: Prepare Your Documents

Place your domain-specific text documents in the documents folder:

```bash
# The documents folder is located at:
backend/src/main/resources/documents/

# Add your .txt files there
cp /path/to/your/documents/*.txt backend/src/main/resources/documents/
```

**Sample document provided:** `cloud_computing.txt` - feel free to add more!

### Document Format Tips:
- Use plain text (.txt) files
- Each file should contain domain-specific content
- More documents = better embeddings
- Recommended: 5-10 documents with 20+ sentences each

## Step 3: Set Up the Backend

```bash
cd backend

# Install dependencies and build
mvn clean install

# This will:
# - Download DJL and PyTorch dependencies
# - Compile the Java code
# - Run tests (if any)
```

**Note:** First build may take 5-10 minutes to download PyTorch native libraries (~200MB).

### Start the Backend Server

```bash
# Option 1: Using Maven
mvn spring-boot:run

# Option 2: Using the JAR
java -jar target/custom-embedding-backend-1.0.0.jar
```

The backend will start on **http://localhost:8080**

**Verify it's running:**
```bash
curl http://localhost:8080/api/health
# Should return: {"status":"healthy","modelTrained":false}
```

## Step 4: Set Up the Frontend

Open a **new terminal window** and navigate to the frontend folder:

```bash
cd frontend

# Install dependencies
npm install

# This will install React and other dependencies
```

### Start the Frontend

```bash
npm start
```

The frontend will start on **http://localhost:3000** and should automatically open in your browser.

## Step 5: Using the Application

### 1. Train the Model

1. Navigate to http://localhost:3000
2. Click the **"🚀 Train Model"** button
3. Wait for training to complete (progress shown in real-time)
   - Default: 300 epochs
   - Typical time: 2-5 minutes depending on document size

### 2. Test Similarities

Once training is complete:

1. Scroll to the **"🔍 Similarity Comparison"** section
2. Enter two words from your vocabulary
   - Auto-complete suggestions are provided
3. Click **"🔍 Calculate Similarity"**
4. View results from both:
   - **Custom Model** (your domain-specific model)
   - **Generic Model** (baseline comparison)

### Example Queries (Cloud Computing Domain):

```
aws ↔ s3          # High similarity (same cloud provider + service)
azure ↔ blob      # High similarity (Azure's object storage)
storage ↔ compute # Low similarity (different service categories)
kubernetes ↔ eks  # Medium similarity (container orchestration)
```

## Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.properties`:

```properties
# Embedding parameters
embedding.dimension=16        # Size of embedding vectors
embedding.margin=2.0          # Contrastive loss margin
embedding.epochs=300          # Training iterations
embedding.learning-rate=0.01  # Optimizer learning rate

# Document processing
documents.folder=src/main/resources/documents
documents.max-distance=5      # Max word distance for pairs
```

### Frontend Configuration

Create `frontend/.env` (optional):

```env
REACT_APP_API_URL=http://localhost:8080/api
```

## Troubleshooting

### Backend Issues

**Port 8080 already in use:**
```bash
# Change port in application.properties
server.port=8081
```

**Out of Memory:**
```bash
# Increase Java heap size
export MAVEN_OPTS="-Xmx2g"
mvn spring-boot:run
```

**PyTorch download fails:**
- Check internet connection
- Try manually downloading from: https://djl.ai/engines/pytorch/pytorch-engine

### Frontend Issues

**Port 3000 already in use:**
```bash
# Choose a different port when prompted, or:
PORT=3001 npm start
```

**CORS errors:**
- Ensure backend is running on http://localhost:8080
- Check `@CrossOrigin` annotation in `EmbeddingController.java`

### Common Issues

**"Model not trained" error:**
- Click "Train Model" button first
- Wait for training to complete (check progress bar)

**"Word not in vocabulary" error:**
- The word must exist in your training documents
- Check the vocabulary list displayed after training
- Words are case-insensitive

**Slow training:**
- Reduce epochs in `application.properties`
- Use fewer documents for testing
- Consider using CPU-optimized PyTorch build

## Development Tips

### Hot Reload

**Backend:**
- Use Spring DevTools (already included)
- Changes auto-reload in development mode

**Frontend:**
- React hot reloading is enabled by default
- Changes appear instantly in browser

### Adding More Documents

1. Add .txt files to `backend/src/main/resources/documents/`
2. Restart backend or re-train model
3. New vocabulary will be automatically extracted

### API Testing

Use curl or Postman to test endpoints:

```bash
# Check health
curl http://localhost:8080/api/health

# Start training
curl -X POST http://localhost:8080/api/train

# Check status
curl http://localhost:8080/api/status

# Calculate similarity
curl -X POST http://localhost:8080/api/similarity/compare \
  -H "Content-Type: application/json" \
  -d '{"word1":"aws","word2":"s3"}'
```

## Next Steps

1. **Add Your Documents**: Replace sample documents with your domain-specific content
2. **Experiment with Parameters**: Adjust embedding dimension, epochs, margin
3. **Compare Results**: Test how custom embeddings differ from generic ones
4. **Extend Functionality**: Add features like:
   - Batch similarity calculation
   - Embedding visualization (t-SNE)
   - Model persistence across restarts
   - Multiple document sets

## Production Deployment

For production use:

1. **Backend:**
   ```bash
   mvn clean package
   java -jar target/custom-embedding-backend-1.0.0.jar
   ```

2. **Frontend:**
   ```bash
   npm run build
   # Serve the build folder with nginx or similar
   ```

3. **Security:**
   - Remove `@CrossOrigin(origins = "*")` and configure properly
   - Add authentication/authorization
   - Use HTTPS
   - Set environment-specific configurations

## Support

For issues or questions:
- Check the main [README.md](README.md)
- Review the blog post: https://navaneethpt.tech/posts/custom_embedding/
- Create an issue in the repository

## License

MIT License - See LICENSE file for details
