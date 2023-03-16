package in.vanna.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration

public class S3Config {
	@Value("${accessKey}")
	private String accessKey;

	@Value("${secretKey}")
	private String secretKey;

	@Value("${region}")
	private String region;

	@Primary
	@Bean
	public AmazonS3 amazonS3Client() {
		return AmazonS3ClientBuilder
				.standard()
				.withRegion(region)
				.withCredentials
				(new AWSStaticCredentialsProvider
						(new BasicAWSCredentials
								(accessKey, secretKey)))
				.build();
	}
	
}
