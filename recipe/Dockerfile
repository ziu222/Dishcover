# Build multi-stage: cần copy toàn bộ monorepo (Maven reactor multi-module đọc <modules> ở pom.xml
# gốc, thiếu bất kỳ module nào cũng fail dù không dùng tới) — build context là thư mục gốc repo:
#   docker build -f recipe/Dockerfile -t dishcover-recipe .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -q -pl recipe -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/recipe/target/*.jar app.jar
USER app
EXPOSE 8083
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5   CMD wget -q --spider http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
