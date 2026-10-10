package com.example.diary.configuration.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.genai.Client;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentResponse;

@Configuration
public class SpringAiConfig {

    // 1. Text Splitter Bean
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(1000)
                .withMinChunkSizeChars(500)
                .withKeepSeparator(true)
                .build();
    }

    // 2. Embedding Model Bean for VectorStore
    @Bean
    public EmbeddingModel embeddingModel(@Value("${spring.ai.google.genai.api-key}") String apiKey) {
        Client client = Client.builder().apiKey(apiKey).build();
        String modelName = "gemini-embedding-2";
        return new AbstractEmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = new ArrayList<>();
                List<String> instructions = request.getInstructions();

                for (int i = 0; i < instructions.size(); i++) {
                    String text = instructions.get(i);
                    com.google.genai.types.EmbedContentConfig config = com.google.genai.types.EmbedContentConfig.builder()
                            .outputDimensionality(768)
                            .build();
                    EmbedContentResponse response = client.models.embedContent(modelName, text, config);
                    List<Float> values = response.embeddings()
                            .flatMap(list -> list.stream().findFirst())
                            .flatMap(ContentEmbedding::values)
                            .orElse(List.of());

                    float[] vector = new float[values.size()];
                    for (int j = 0; j < values.size(); j++) {
                        vector[j] = values.get(j);
                    }
                    embeddings.add(new Embedding(vector, i));
                }
                return new EmbeddingResponse(embeddings, new EmbeddingResponseMetadata());
            }

            @Override
            public float[] embed(Document document) {
                return embed(document.getFormattedContent(MetadataMode.NONE));
            }

            @Override
            public int dimensions() {
                return 768;
            }
        };
    }

    // 3. ObjectMapper Bean
    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
