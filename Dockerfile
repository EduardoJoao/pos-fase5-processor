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

# Etapa 2: Imagem final com Ubuntu (melhor compatibilidade com FFmpeg)
FROM eclipse-temurin:17-jre

# Instalar FFmpeg e dependências necessárias no Ubuntu
RUN apt-get update && apt-get install -y \
    ffmpeg \
    libavcodec-dev \
    libavformat-dev \
    libavutil-dev \
    libswscale-dev \
    libswresample-dev \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r spring && useradd -r -g spring spring

# Verificar se FFmpeg foi instalado corretamente
RUN ffmpeg -version

# Definir variáveis de ambiente para bibliotecas
ENV LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:$LD_LIBRARY_PATH

USER spring:spring
WORKDIR /app

# Copia apenas o JAR gerado
COPY --from=build --chown=spring:spring /app/target/pos-fase5-processor-0.0.1-SNAPSHOT.jar app.jar

# Configurações JVM ajustadas para evitar problemas com bibliotecas nativas
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.awt.headless=true -Djna.library.path=/usr/lib/x86_64-linux-gnu"

# Expõe a porta da aplicação
EXPOSE 8080

# Define o ponto de entrada otimizado
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]