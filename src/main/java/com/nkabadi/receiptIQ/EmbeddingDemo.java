package com.nkabadi.receiptIQ;


import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingDemo {

	// @Bean  /*Umcomment this to run the demo. Basically demo of how the embedding works. */
	CommandLineRunner printEmbedding(EmbeddingModel embeddingModel) {
		return args -> {
			float[] snacks = embeddingModel.embed("I bought snacks");
			float[] chips = embeddingModel.embed("chips and cookies from the store");
			float[] joistHanger = embeddingModel.embed("joist hanger and concrete mix");

			System.out.println("Embedding vector length: " + snacks.length);
			System.out.println("Snacks vs chips similarity: " + cosineSimilarity(snacks, chips));
			System.out.println("Snacks vs joist hanger similarity: " + cosineSimilarity(snacks, joistHanger));
			System.out.println("Chips vs joist hanger similarity: " + cosineSimilarity(chips, joistHanger));
		};
	}

	private static double cosineSimilarity(float[] first, float[] second) {
		if (first.length != second.length) {
			throw new IllegalArgumentException("Embedding vectors must have the same length.");
		}

		double dotProduct = 0.0;
		double firstMagnitude = 0.0;
		double secondMagnitude = 0.0;
		for (int index = 0; index < first.length; index++) {
			dotProduct += first[index] * (double) second[index];
			firstMagnitude += first[index] * (double) first[index];
			secondMagnitude += second[index] * (double) second[index];
		}

		return dotProduct / (Math.sqrt(firstMagnitude) * Math.sqrt(secondMagnitude));
	}
}
