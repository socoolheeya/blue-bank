#!/bin/bash

echo "Building all services..."

# Build infrastructure services
./gradlew :infra:eureka-server:bootJar
./gradlew :infra:api-gateway:bootJar

# Build application services
./gradlew :app:account:bootJar
./gradlew :app:deposit:bootJar
./gradlew :app:loan:bootJar
./gradlew :app:card:bootJar

echo "All services built successfully!"
