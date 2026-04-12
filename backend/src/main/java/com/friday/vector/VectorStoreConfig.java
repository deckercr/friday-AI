package com.friday.vector;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfig {

    @Bean
    VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel,
            @Value("${app.vector.dimensions:1024}") int dimensions) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
            .dimensions(dimensions)
            .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
            .initializeSchema(true)
            .build();
    }
}
