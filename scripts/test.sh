#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Default values
RUN_UNIT_TESTS=true
RUN_E2E_TESTS=true
USE_DOCKER=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --unit-only)
            RUN_E2E_TESTS=false
            shift
            ;;
        --e2e-only)
            RUN_UNIT_TESTS=false
            shift
            ;;
        --docker)
            USE_DOCKER=true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}Running Arktrio tests...${NC}"

if [ "$RUN_UNIT_TESTS" = true ]; then
    echo -e "${GREEN}Running unit tests...${NC}"
    if [ "$USE_DOCKER" = true ]; then
        docker run --rm -v $(pwd):/app -w /app arktrio-center sbt test
    else
        sbt test
    fi
fi

if [ "$RUN_E2E_TESTS" = true ]; then
    echo -e "${GREEN}Running end-to-end tests...${NC}"
    
    # Start services in the background
    if [ "$USE_DOCKER" = true ]; then
        echo -e "${BLUE}Starting services with Docker...${NC}"
        docker-compose -f docker-compose.test.yml up -d
        
        # Wait for services to be ready
        echo -e "${BLUE}Waiting for services to be ready...${NC}"
        until curl -s http://localhost:2236/health > /dev/null; do
            echo "Waiting for center service..."
            sleep 1
        done
        
        until curl -s http://localhost:2237/health > /dev/null; do
            echo "Waiting for edge service..."
            sleep 1
        done
        
        # Run tests
        docker-compose -f docker-compose.test.yml run test-runner python -m pytest tests/e2e/
        
        # Cleanup
        docker-compose -f docker-compose.test.yml down
    else
        echo -e "${BLUE}Starting services with JARs...${NC}"
        
        # Start center
        java -Dconfig.file=center.conf -XX:+UseZGC -XX:+ZGenerational -jar center/target/scala-*/arktrio-center.jar &
        CENTER_PID=$!
        
        # Start edge
        ARKTRIO_CENTER_STATIC_HOST=localhost java -Dconfig.file=edge.conf -XX:+UseZGC -XX:+ZGenerational -jar edge/target/scala-*/arktrio-edge.jar &
        EDGE_PID=$!
        
        # Wait for services to be ready
        echo -e "${BLUE}Waiting for services to be ready...${NC}"
        until curl -s http://localhost:2236/health > /dev/null; do
            echo "Waiting for center service..."
            sleep 1
        done
        
        until curl -s http://localhost:2237/health > /dev/null; do
            echo "Waiting for edge service..."
            sleep 1
        done
        
        # Run tests
        python -m pytest tests/e2e/
        
        # Cleanup
        kill $CENTER_PID $EDGE_PID
    fi
fi

echo -e "${GREEN}All tests completed!${NC}" 