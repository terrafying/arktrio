#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Building Arktrio...${NC}"

# Check if Docker is installed
if command -v docker &> /dev/null; then
    echo -e "${GREEN}Building Docker images...${NC}"
    docker build -t arktrio-center -f docker/center.dockerfile .
    docker build -t arktrio-edge -f docker/edge.dockerfile .
else
    echo "Docker not found, skipping Docker builds"
fi

# Check if sbt is installed
if command -v sbt &> /dev/null; then
    echo -e "${GREEN}Building JARs...${NC}"
    sbt clean assembly
    echo -e "${GREEN}JARs built successfully:${NC}"
    echo "- center/target/scala-*/arktrio-center.jar"
    echo "- edge/target/scala-*/arktrio-edge.jar"
else
    echo "sbt not found, skipping JAR builds"
fi

echo -e "${BLUE}Build complete!${NC}" 