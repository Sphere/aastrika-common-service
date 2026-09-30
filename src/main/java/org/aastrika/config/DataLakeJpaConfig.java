package org.aastrika.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManagerFactory;

/**
 * The data lake channel — a second PostgreSQL database, typically on a different host, holding
 * ETL-populated tables rather than data this service writes. The leaderboard is its first tenant
 * ({@code leaderboard_table} is produced by an external ETL job), but nothing here is
 * leaderboard-specific.
 *
 * <p>Membership is decided entirely by package, so this class never needs editing: put entities in
 * {@link #ENTITY_PACKAGE} and repositories in {@link #REPOSITORY_PACKAGE}. Both are siblings of
 * {@code org.aastrika.entity} / {@code org.aastrika.repository} rather than subpackages of them,
 * which is what lets both persistence units scan by package with no include/exclude filters —
 * neither scan can reach the other's types. Anything under the primary packages stays on the primary
 * database, also with no configuration change.
 *
 * <p>Keeping both configurations filter-free matters beyond tidiness: Spring Data disables its
 * strict multi-store detection as soon as a configuration declares any filter, and without that
 * detection the primary JPA scan starts claiming the Cassandra repositories.
 *
 * <p>Configured under {@code data-lake.datasource.*}, overridable per environment with the matching
 * {@code DATA_LAKE_DB_*} variables. {@code DATA_LAKE_DB_PASSWORD} is required — it has no default,
 * so startup fails fast rather than authenticating with a placeholder.
 *
 * <p>Nothing here is {@code @Primary}: injection points must qualify explicitly, and
 * {@code @Transactional} on code using this channel must name {@code dataLakeTransactionManager}.
 * {@code hibernate.hbm2ddl.auto} is pinned to {@code none} so this unit can never emit DDL
 * regardless of the shared {@code spring.jpa.*} setting (TRIAGE T-002) — the service only reads
 * these tables.
 *
 * <p>Package names stay all-lowercase ({@code datalake}) per Java convention; the two words are
 * separated everywhere that convention allows it — {@code DataLake} in types, {@code dataLake} in
 * bean names, {@code data-lake} in property keys, {@code DATA_LAKE} in environment variables.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = DataLakeJpaConfig.REPOSITORY_PACKAGE,
        entityManagerFactoryRef = "dataLakeEntityManagerFactory",
        transactionManagerRef = "dataLakeTransactionManager")
public class DataLakeJpaConfig {

    /** Entities in this package are mapped by this channel. */
    public static final String ENTITY_PACKAGE = "org.aastrika.datalake.entity";

    /** Repositories in this package are bound to this channel. */
    public static final String REPOSITORY_PACKAGE = "org.aastrika.datalake.repository";

    @Bean
    @ConfigurationProperties("data-lake.datasource")
    public DataSourceProperties dataLakeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("data-lake.datasource.hikari")
    public HikariDataSource dataLakeDataSource(
            @Qualifier("dataLakeDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean dataLakeEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dataLakeDataSource") HikariDataSource dataSource) {
        return builder.dataSource(dataSource)
                .packages(ENTITY_PACKAGE)
                .persistenceUnit("dataLake")
                .properties(Map.of("hibernate.hbm2ddl.auto", "none"))
                .build();
    }

    @Bean
    public PlatformTransactionManager dataLakeTransactionManager(
            @Qualifier("dataLakeEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
