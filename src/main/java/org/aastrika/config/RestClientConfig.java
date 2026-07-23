package org.aastrika.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * {@link RestTemplate} for the content/learning HTTP calls. Uses the JDK {@link HttpClient} factory
 * because the default {@code SimpleClientHttpRequestFactory} cannot send {@code PATCH} (needed for
 * the content system-update call). Read timeout is generous — the content service is slow.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate contentRestTemplate() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));
        return new RestTemplate(factory);
    }
}
