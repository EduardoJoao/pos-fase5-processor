# Etapa 1: Compilação e build da aplicação
FROM maven:3.8-eclipse-temurin-17-alpine AS build

# Define o diretório de trabalho
WORKDIR /app

# Copia apenas os arquivos de configuração do Maven primeiro (para melhor cache)
COPY pom.xml .
COPY src/main/resources/application.properties src/main/resources/application.properties

# Baixa as dependências (será cacheado se não houver mudanças no pom.xml)
RUN mvn dependency:go-offline -B

# Agora copia o código fonte e compila
COPY src/ src/
RUN mvn package -DskipTests

# Etapa 2: Imagem final com FFmpeg
FROM eclipse-temurin:17-jre-alpine

# Instalar FFmpeg e dependências necessárias
RUN apk --no-cache add \
    ffmpeg \
    ca-certificates \
    && rm -rf /var/cache/apk/* \
    && addgroup -S spring && adduser -S spring -G spring

# Verificar se FFmpeg foi instalado corretamente
RUN ffmpeg -version

USER spring:spring
WORKDIR /app

# Copia apenas o JAR gerado
COPY --from=build --chown=spring:spring /app/target/pos-fase5-processor-0.0.1-SNAPSHOT.jar app.jar

# Configurações JVM para contêineres (aumentadas para processamento de vídeo)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions -XX:+UseZGC"

# Expõe a porta da aplicação
EXPOSE 8080

# Define o ponto de entrada otimizado
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]