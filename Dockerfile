# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

# 1. copy all files
COPY backend/mvnw backend/mvnw
COPY backend/.mvn backend/.mvn
COPY backend/pom.xml backend/pom.xml
COPY frontend frontend
COPY backend/src backend/src

WORKDIR /workspace/backend

RUN sed -i 's/\r$//' ./mvnw && chmod +x ./mvnw

# package
RUN ./mvnw -B -DskipTests clean package

# run phase
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

ENV TZ=Asia/Shanghai
ENV SPRING_PROFILES_ACTIVE=prod
ENV HERLINK_UPLOAD_DIR=/app/uploads
ENV HERLINK_FRONTEND_DIR=/app/frontend
ENV JAVA_OPTS=""

RUN mkdir -p /app/uploads
COPY --from=build /workspace/backend/target/*.jar /app/app.jar
COPY --from=build /workspace/frontend /app/frontend

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Duser.timezone=Asia/Shanghai -jar /app/app.jar"]
