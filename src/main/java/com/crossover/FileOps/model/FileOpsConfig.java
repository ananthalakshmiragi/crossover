package com.crossover.FileOps.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
public class FileOpsConfig {
	
	@Value("${fupl.access.key.id}")
    private String s3KeyId;

    @Value("${fupl.secret.access.key}")
    private String s3SecretKey;

    @Value("${fupl.s3.region}")
    private String s3Region;

    @Value("${fupl.s3.bucket}")
    private String s3Bucket;
    
    @Value("${fupl.maxFileSize}")
	private static long maxFileSize;

    @Bean(name = "s3KeyId")
    public String getS3KeyId() {
        return s3KeyId;
    }
    
    @Bean(name = "s3SecretKey")
    public String getS3SecretKey() {
        return s3SecretKey;
    }

    @Bean(name = "s3Region")
    public Region getS3Region() {
        return Region.getRegion(Regions.fromName(s3Region));
    }
    
    @Bean
    public AmazonS3 getAmazonS3Cient() {
        final BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(this.s3KeyId, this.s3SecretKey);
        return AmazonS3ClientBuilder
                .standard()
                .withRegion(Regions.fromName(s3Region))
                .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                .build();
    }
    
    @Bean(name = "s3CredentialsProvider")
    public AWSCredentialsProvider getAWSCredentials() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(this.s3KeyId, this.s3SecretKey);
        return new AWSStaticCredentialsProvider(awsCredentials);
    }

    @Bean(name = "s3Bucket")
    public String getS3Bucket() {
        return s3Bucket;
    }

    @Bean(name = "maxFileSize")
    public long getMaxFileSize() {
        return maxFileSize;
    }
}
