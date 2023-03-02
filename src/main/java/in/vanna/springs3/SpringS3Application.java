package in.vanna.springs3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;

@SpringBootApplication
@OpenAPIDefinition
public class SpringS3Application {

	public static void main(String[] args) {
		SpringApplication.run(SpringS3Application.class, args);
	}

}
