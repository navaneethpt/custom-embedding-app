#!/bin/bash

# Custom Embedding App - Stop Script
# Stops both backend and frontend servers

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

echo "Stopping Custom Embedding App servers..."
echo ""

# Stop backend
if [ -f backend.pid ]; then
    BACKEND_PID=$(cat backend.pid)
    if kill -0 $BACKEND_PID 2>/dev/null; then
        kill $BACKEND_PID
        print_success "Backend stopped (PID: $BACKEND_PID)"
    else
        print_info "Backend was not running"
    fi
    rm backend.pid
else
    print_info "No backend PID file found"
fi

# Stop frontend
if [ -f frontend.pid ]; then
    FRONTEND_PID=$(cat frontend.pid)
    if kill -0 $FRONTEND_PID 2>/dev/null; then
        kill $FRONTEND_PID
        print_success "Frontend stopped (PID: $FRONTEND_PID)"
    else
        print_info "Frontend was not running"
    fi
    rm frontend.pid
else
    print_info "No frontend PID file found"
fi

# Kill any remaining processes on ports
print_info "Checking for processes on ports 8080 and 3000..."

# Kill on port 8080 (backend)
BACKEND_PROC=$(lsof -ti:8080)
if [ ! -z "$BACKEND_PROC" ]; then
    kill -9 $BACKEND_PROC 2>/dev/null
    print_success "Killed process on port 8080"
fi

# Kill on port 3000 (frontend)
FRONTEND_PROC=$(lsof -ti:3000)
if [ ! -z "$FRONTEND_PROC" ]; then
    kill -9 $FRONTEND_PROC 2>/dev/null
    print_success "Killed process on port 3000"
fi

echo ""
print_success "All servers stopped successfully"
