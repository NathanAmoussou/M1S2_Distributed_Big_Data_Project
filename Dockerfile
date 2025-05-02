# Dockerfile (for Java App in project root)

# Stage 1: Build the application using Maven
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
# !!! Double-check this JAR name after running 'mvn package' !!!
RUN mvn package -DskipTests # Skip tests during build

# Stage 2: Create the final image using a JRE
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the fat JAR from the builder stage
COPY --from=builder /build/target/M1S2_Distributed_Big_Data_Project-1.0-SNAPSHOT.jar ./app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "./app.jar"]