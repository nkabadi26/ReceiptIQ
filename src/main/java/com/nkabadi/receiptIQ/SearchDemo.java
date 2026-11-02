package com.nkabadi.receiptIQ;

import java.util.List;
import java.util.Locale;

import com.nkabadi.receiptIQ.repository.LineItemRepository;
import com.nkabadi.receiptIQ.util.VectorTextUtils;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchDemo {

	// @Bean /*Umcomment this to run the demo. Basically demo of how the vector serach works. */
	CommandLineRunner runSearchTest(EmbeddingModel embeddingModel, LineItemRepository lineItemRepository) {
		return args -> {
			List<String> questions = List.of(
					"what snacks did I buy?",
					"wood and building supplies",
					"things for the baby",
					"restaurant meals");

			for (String question : questions) {
				float[] questionVector = embeddingModel.embed(question);
				System.out.println(question);
				for (Object[] result : lineItemRepository.findNearest(VectorTextUtils.toVectorText(questionVector), 5)) {
					double distance = ((Number) result[7]).doubleValue();
					System.out.printf(Locale.US, "%.4f %s%n", distance, result[4]);
				}
			}
		};
	}
}
