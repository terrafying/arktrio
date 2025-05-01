# Arktrio: Distributed Multi-Agent Messaging Framework

[![Scala CI](https://github.com/terrafying/arktrio/actions/workflows/scala-ci.yaml/badge.svg?branch=main)](https://github.com/terrafying/arktrio/actions/workflows/scala-ci.yaml)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

Arktrio is a distributed messaging framework designed to connect various agent-based software, with a focus on AI-powered multi-agent systems and semantic mesh networks. It combines the power of Semantic Kernel for AI capabilities with a robust mesh networking infrastructure.

## Core Components

### 1. Center (Message Broker)
- Handles message routing and distribution
- Implements network culling for large-scale agent communication
- Manages semantic mesh topology
- Provides AI service orchestration

### 2. Edge (Agent Connector)
- Deploys as a sidecar for agent-based software
- Handles coordinate transformation and time synchronization
- Provides local REST API for agent communication
- Integrates with Semantic Kernel for local AI processing

### 3. Semantic Mesh
- Self-organizing network topology
- Dynamic agent discovery and routing
- Semantic message routing based on agent capabilities
- AI-powered message optimization

## Features

### AI Integration
- Semantic Kernel integration for AI capabilities
- Multi-agent collaboration and reasoning
- Dynamic skill discovery and composition
- Memory and context management

### Mesh Networking
- Self-distributing mesh topology
- Dynamic node discovery and routing
- Message batching and optimization
- Fault tolerance and recovery

### Agent Communication
- Type-safe Protocol Buffers communication
- Streaming responses for real-time updates
- Capability-based agent matching
- Context-aware message routing

## Building

### Docker

1. `git checkout <release tag>`
1. `docker build -t arktrio-center -f docker/center.dockerfile .`
1. `docker build -t arktrio-edge -f docker/edge.dockerfile .`

### JAR

1. Install Java Development Kit (recommended: [Eclipse Temurin 21 LTS](https://adoptium.net/temurin/releases/?variant=openjdk21&jvmVariant=hotspot))
1. Install [sbt](https://www.scala-sbt.org/download)
1. Install [Node.js](https://nodejs.org/en/download/package-manager) (recommended: v22)
1. `git checkout <release tag>`
1. `cd arktrio`
1. `sbt center/assembly`
1. Pick up `arktrio-center.jar` from `center/target/scala-*.*.*/`
1. `sbt viewer/package edge/assembly`
1. Pick up `arktrio-edge.jar` from `edge/target/scala-*.*.*/`

## Running

### Docker

- `docker run [--network host | -p 2236:2236] [-v $(pwd)/center.conf:/etc/opt/arktrio/center.conf] arktrio-center [arg]...`
- `docker run [--network host | -p 2237:2237] [-v $(pwd)/edge.conf:/etc/opt/arktrio/edge.conf] -e ARKTRIO_CENTER_STATIC_HOST=<CENTER_HOST> arktrio-edge`

### JAR

- `java [-Dconfig.file=center.conf] -XX:+UseZGC -XX:+ZGenerational -jar arktrio-center.jar`
- `ARKTRIO_CENTER_STATIC_HOST=<CENTER_HOST> java [-Dconfig.file=edge.conf] -XX:+UseZGC -XX:+ZGenerational -jar arktrio-edge.jar [arg]...`

## Configuration

The framework supports three core components and five operational modes:

### Core Components
1. Messaging
   - Batch processing
   - Timeout handling
   - Message routing

2. Monitoring
   - Health checks
   - Performance metrics
   - Resource usage

3. Persistence
   - State management
   - Checkpointing
   - Recovery

### Operational Modes
1. Development
   - Debug logging
   - Local testing
   - Rapid iteration

2. Testing
   - Mock services
   - Integration testing
   - Performance testing

3. Staging
   - Validation
   - Pre-production
   - Load testing

4. Production
   - Optimization
   - Monitoring
   - High availability

5. Maintenance
   - Read-only mode
   - Backup
   - Updates

## API Reference

### Center API
- gRPC Server: localhost:2236
- Health Check: [localhost:2236/health](http://localhost:2236/health)
- Prometheus Exporter: [localhost:2236/metrics](http://localhost:2236/metrics)

### Edge API
- REST API Server: localhost:2237/api/
- REST Docs: [localhost:2237/docs/](http://localhost:2237/docs/)
- Health Check: [localhost:2237/health](http://localhost:2237/health)
- Prometheus Exporter: [localhost:2237/metrics](http://localhost:2237/metrics)
- Neighbors Viewer: [localhost:2237/viewer/](http://localhost:2237/viewer/)

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the terms of the license included in the repository.
