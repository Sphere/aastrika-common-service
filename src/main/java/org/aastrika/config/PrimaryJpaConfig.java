package org.aastrika.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManagerFactory;

/**
 * The default PostgreSQL channel — unchanged in behaviour. Same {@code spring.datasource.*} and
 * {@code spring.jpa.*} property keys as before, same package scanning, and {@code @Primary} so every
 * unqualified {@code DataSource} / {@code EntityManagerFactory} / {@code @Transactional} injection
 * point still resolves here. Entities added to {@code org.aastrika.entity} and repositories added to
 * {@code org.aastrika.repository} land on this channel automatically.
 *
 * <p>This has to be declared explicitly only because a second unit exists: Spring Boot's
 * {@code JpaBaseConfiguration.entityManagerFactory} is
 * {@code @ConditionalOnMissingBean(LocalContainerEntityManagerFactoryBean.class)} and
 * {@code DataSourceAutoConfiguration} is {@code @ConditionalOnMissingBean(DataSource.class)}, so
 * declaring the data lake channel makes Boot back off from auto-configuring this one.
 *
 * <p>Neither scan needs a filter. {@link DataLakeJpaConfig} owns {@code org.aastrika.datalake.*},
 * which is a sibling of these packages rather than a subpackage, so the recursive scans cannot
 * overlap. That also keeps Spring Data's strict multi-store detection active — declaring any
 * include/exclude filter switches it off — which is what stops this JPA scan from claiming the
 * Cassandra repositories that share {@code org.aastrika.repository}.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "org.aastrika.repository",
        entityManagerFactoryRef = "primaryEntityManagerFactory",
        transactionManagerRef = "primaryTransactionManager")
public class PrimaryJpaConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource primaryDataSource(
            @Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("primaryDataSource") HikariDataSource dataSource) {
        return builder.dataSource(dataSource)
                .packages("org.aastrika.entity")
                .persistenceUnit("primary")
                .build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
