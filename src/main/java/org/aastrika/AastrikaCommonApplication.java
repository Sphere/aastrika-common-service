package org.aastrika;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Elasticsearch auto-configurations are excluded here rather than via
 * {@code spring.autoconfigure.exclude} in application.properties: the property form can be silently
 * dropped by any environment that overrides that key or replaces the config location (which is what
 * broke stage), whereas the annotation is compiled into the jar and cannot be overridden.
 *
 * <p>Why the exclusion is needed: {@code spring-data-opensearch-starter} pulls in
 * spring-data-elasticsearch but excludes {@code co.elastic.clients:*}. Spring Boot's
 * {@code ElasticsearchDataAutoConfiguration} is {@code @ConditionalOnClass(ElasticsearchTemplate.class)},
 * which matches, and it imports {@code ElasticsearchDataConfiguration$ReactiveRestClientConfiguration},
 * whose bean signatures reference {@code co.elastic.clients.ApiClient} — absent at runtime, so
 * introspecting it fails with {@code NoClassDefFoundError} during context refresh.
 *
 * <p>Class names verified against spring-boot-autoconfigure 3.4.2. {@code excludeName} (string form)
 * is used deliberately so the excluded classes are never loaded.
 */
@SpringBootApplication(excludeName = {
        "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.elasticsearch.ReactiveElasticsearchClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration"
})
public class AastrikaCommonApplication {

    public static void main(String[] args) {
        SpringApplication.run(AastrikaCommonApplication.class, args);
    }
}
