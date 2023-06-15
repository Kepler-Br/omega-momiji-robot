package com.momiji.bot.config


import java.time.Duration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.retry.RetryPolicy
import software.amazon.awssdk.http.SdkHttpConfigurationOption
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.utils.AttributeMap


@Configuration
class S3ClientConfiguration(
    private val s3ConfigurationProperties: S3ConfigurationProperties
) {

    @Bean
    fun s3SyncGatewayClient(): S3Client {
        val serviceConfiguration = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .chunkedEncodingEnabled(false)
            .checksumValidationEnabled(false)
            .build()

        val credentials = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                s3ConfigurationProperties.accessKeyId,
                s3ConfigurationProperties.secretAccessKey
            )
        )

        val endpoint = UriComponentsBuilder.newInstance()
            .scheme(s3ConfigurationProperties.scheme)
            .host(s3ConfigurationProperties.host)
            .port(s3ConfigurationProperties.port)
            .path(s3ConfigurationProperties.path)
            .build()
            .toUri()

        val configuration = ClientOverrideConfiguration.builder()
            .retryPolicy(RetryPolicy.none())
            .build()

        val sdkClient = ApacheHttpClient.builder()
            .connectionTimeout(Duration.ofSeconds(1))
            .buildWithDefaults(
                AttributeMap.builder()
                    .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                    .build()
            )

        return S3Client.builder()
            .region(Region.of(s3ConfigurationProperties.region))
            .httpClient(sdkClient)
            .endpointOverride(endpoint)
            .serviceConfiguration(serviceConfiguration)
            .credentialsProvider(credentials)
            .overrideConfiguration(configuration)
            .build()
    }
}

