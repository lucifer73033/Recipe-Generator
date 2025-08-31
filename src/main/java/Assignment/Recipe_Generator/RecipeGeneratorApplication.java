package Assignment.Recipe_Generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class RecipeGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecipeGeneratorApplication.class, args);
	}

}
