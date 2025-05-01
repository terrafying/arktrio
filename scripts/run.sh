#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Default values
CENTER_HOST="localhost"
CENTER_PORT=2236
EDGE_PORT=2237
USE_DOCKER=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --docker)
            USE_DOCKER=true
            shift
            ;;
        --center-host)
            CENTER_HOST="$2"
            shift 2
            ;;
        --center-port)
            CENTER_PORT="$2"
            shift 2
            ;;
        --edge-port)
            EDGE_PORT="$2"
            shift 2
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Function to check if a port is in use
check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null ; then
        echo -e "${RED}Port $1 is already in use${NC}"
        exit 1
    fi
}

# Check ports before starting
check_port $CENTER_PORT
check_port $EDGE_PORT

if [ "$USE_DOCKER" = true ]; then
    echo -e "${BLUE}Starting services with Docker...${NC}"
    
    # Start center
    echo -e "${GREEN}Starting center service...${NC}"
    docker run -d --name arktrio-center \
        -p $CENTER_PORT:2236 \
        -v $(pwd)/center.conf:/etc/opt/arktrio/center.conf \
        arktrio-center

    # Start edge
    echo -e "${GREEN}Starting edge service...${NC}"
    docker run -d --name arktrio-edge \
        -p $EDGE_PORT:2237 \
        -v $(pwd)/edge.conf:/etc/opt/arktrio/edge.conf \
        -e ARKTRIO_CENTER_STATIC_HOST=$CENTER_HOST \
        arktrio-edge

    echo -e "${GREEN}Services started!${NC}"
    echo "Center: http://localhost:$CENTER_PORT"
    echo "Edge: http://localhost:$EDGE_PORT"
else
    echo -e "${BLUE}Starting services with JARs...${NC}"
    
    # Start center
    echo -e "${GREEN}Starting center service...${NC}"
    java -Dconfig.file=center.conf -XX:+UseZGC -XX:+ZGenerational -jar center/target/scala-*/arktrio-center.jar &
    CENTER_PID=$!

    # Start edge
    echo -e "${GREEN}Starting edge service...${NC}"
    ARKTRIO_CENTER_STATIC_HOST=$CENTER_HOST java -Dconfig.file=edge.conf -XX:+UseZGC -XX:+ZGenerational -jar edge/target/scala-*/arktrio-edge.jar &
    EDGE_PID=$!

    echo -e "${GREEN}Services started!${NC}"
    echo "Center: http://localhost:$CENTER_PORT (PID: $CENTER_PID)"
    echo "Edge: http://localhost:$EDGE_PORT (PID: $EDGE_PID)"
    
    # Function to handle cleanup on script exit
    cleanup() {
        echo -e "${BLUE}Shutting down services...${NC}"
        kill $CENTER_PID $EDGE_PID 2>/dev/null || true
    }
    trap cleanup EXIT
fi

# Wait for services to be ready
echo -e "${BLUE}Waiting for services to be ready...${NC}"
until curl -s http://localhost:$CENTER_PORT/health > /dev/null; do
    echo "Waiting for center service..."
    sleep 1
done

until curl -s http://localhost:$EDGE_PORT/health > /dev/null; do
    echo "Waiting for edge service..."
    sleep 1
done

echo -e "${GREEN}All services are ready!${NC}"
echo "Press Ctrl+C to stop the services"
wait 