package in.vanna;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;

@SpringBootApplication
@OpenAPIDefinition
public class S3MicroservicesApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(S3MicroservicesApiApplication.class, args);
	}

}
