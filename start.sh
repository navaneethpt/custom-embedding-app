#!/bin/bash

# Custom Embedding App - Quick Start Script
# This script helps you get started quickly

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check Java
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 17 ]; then
            print_success "Java $JAVA_VERSION found"
        else
            print_error "Java 17 or higher required (found Java $JAVA_VERSION)"
            exit 1
        fi
    else
        print_error "Java not found. Please install Java 17 or higher"
        exit 1
    fi
    
    # Check Maven
    if command -v mvn &> /dev/null; then
        MVN_VERSION=$(mvn -version | head -n 1 | awk '{print $3}')
        print_success "Maven $MVN_VERSION found"
    else
        print_error "Maven not found. Please install Maven 3.6+"
        exit 1
    fi
    
    # Check Node.js
    if command -v node &> /dev/null; then
        NODE_VERSION=$(node -v)
        print_success "Node.js $NODE_VERSION found"
    else
        print_error "Node.js not found. Please install Node.js 16+"
        exit 1
    fi
    
    # Check npm
    if command -v npm &> /dev/null; then
        NPM_VERSION=$(npm -v)
        print_success "npm $NPM_VERSION found"
    else
        print_error "npm not found. Please install npm"
        exit 1
    fi
    
    echo ""
}

# Setup backend
setup_backend() {
    print_header "Setting Up Backend"
    
    cd backend
    
    print_info "Installing Maven dependencies..."
    mvn clean install -DskipTests
    
    if [ $? -eq 0 ]; then
        print_success "Backend setup complete"
    else
        print_error "Backend setup failed"
        exit 1
    fi
    
    cd ..
    echo ""
}

# Setup frontend
setup_frontend() {
    print_header "Setting Up Frontend"
    
    cd frontend
    
    print_info "Installing npm dependencies..."
    npm install
    
    if [ $? -eq 0 ]; then
        print_success "Frontend setup complete"
    else
        print_error "Frontend setup failed"
        exit 1
    fi
    
    cd ..
    echo ""
}

# Start backend
start_backend() {
    print_header "Starting Backend Server"
    
    cd backend
    
    print_info "Starting Spring Boot application..."
    print_info "Backend will run on http://localhost:8080"
    
    mvn spring-boot:run &
    BACKEND_PID=$!
    
    # Wait for backend to start
    print_info "Waiting for backend to start..."
    sleep 10
    
    # Check if backend is running
    if curl -s http://localhost:8080/api/health > /dev/null; then
        print_success "Backend is running (PID: $BACKEND_PID)"
        echo $BACKEND_PID > ../backend.pid
    else
        print_error "Backend failed to start"
        kill $BACKEND_PID 2>/dev/null
        exit 1
    fi
    
    cd ..
    echo ""
}

# Start frontend
start_frontend() {
    print_header "Starting Frontend Server"
    
    cd frontend
    
    print_info "Starting React development server..."
    print_info "Frontend will run on http://localhost:3000"
    
    npm start &
    FRONTEND_PID=$!
    
    print_success "Frontend is starting (PID: $FRONTEND_PID)"
    echo $FRONTEND_PID > ../frontend.pid
    
    cd ..
    echo ""
}

# Display next steps
display_next_steps() {
    print_header "Setup Complete!"
    
    echo ""
    print_success "Both servers are running:"
    echo "  • Backend:  http://localhost:8080"
    echo "  • Frontend: http://localhost:3000"
    echo ""
    
    print_info "Next Steps:"
    echo "  1. Open http://localhost:3000 in your browser"
    echo "  2. Click 'Train Model' to train on the sample documents"
    echo "  3. Once training completes, test word similarities"
    echo "  4. Add your own .txt files to backend/src/main/resources/documents/"
    echo ""
    
    print_info "To stop the servers:"
    echo "  ./stop.sh"
    echo "  or manually: kill \$(cat backend.pid frontend.pid)"
    echo ""
    
    print_warning "Keep this terminal open. Closing it will stop the servers."
    echo ""
}

# Main execution
main() {
    clear
    
    print_header "Custom Domain-Specific Embedding System"
    echo "Automated Setup & Launch Script"
    echo ""
    
    # Check if already running
    if [ -f backend.pid ] || [ -f frontend.pid ]; then
        print_warning "Servers may already be running"
        print_info "Run './stop.sh' first if you want to restart"
        read -p "Continue anyway? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 0
        fi
    fi
    
    # Run setup steps
    check_prerequisites
    
    read -p "Install backend dependencies? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        setup_backend
    fi
    
    read -p "Install frontend dependencies? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        setup_frontend
    fi
    
    read -p "Start both servers now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        start_backend
        start_frontend
        display_next_steps
        
        # Keep script running
        wait
    else
        print_info "Setup complete. Run this script again to start servers."
    fi
}

# Run main function
main
