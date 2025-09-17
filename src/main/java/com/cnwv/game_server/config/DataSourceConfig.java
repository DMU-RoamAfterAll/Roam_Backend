package com.cnwv.game_server.config;

import com.cnwv.game_server.shard.RoutingDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    // ===== 샤드 프로퍼티 (테스트에서 비어 있을 수 있으므로 기본값 허용)
    @Value("${spring.shard.datasources.s0.url:}")
    private String s0Url;
    @Value("${spring.shard.datasources.s0.username:}")
    private String s0User;
    @Value("${spring.shard.datasources.s0.password:}")
    private String s0Pass;

    @Value("${spring.shard.datasources.s1.url:}")
    private String s1Url;
    @Value("${spring.shard.datasources.s1.username:}")
    private String s1User;
    @Value("${spring.shard.datasources.s1.password:}")
    private String s1Pass;

    // ===== 단일 DS (샤드 미설정 시 fallback)
    @Value("${spring.datasource.url}")
    private String baseUrl;
    @Value("${spring.datasource.username}")
    private String baseUser;
    @Value("${spring.datasource.password}")
    private String basePass;

    // ✅ HikariDataSource가 아니라 HikariConfig를 빈으로 등록 (검증에 안 걸림)
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        return new HikariConfig();
    }

    @Bean(name = "routingDataSource")
    public DataSource routingDataSource(HikariConfig baseCfg) {
        boolean shardReady =
                notBlank(s0Url) && notBlank(s0User) && notBlank(s0Pass) &&
                        notBlank(s1Url) && notBlank(s1User) && notBlank(s1Pass);

        Map<Object, Object> targets = new HashMap<>();
        RoutingDataSource rds = new RoutingDataSource();

        if (shardReady) {
            targets.put("s0", buildDs(baseCfg, s0Url, s0User, s0Pass));
            targets.put("s1", buildDs(baseCfg, s1Url, s1User, s1Pass));
            rds.setTargetDataSources(targets);
            rds.setDefaultTargetDataSource(targets.get("s0"));
        } else {
            // 테스트/로컬에서 샤드 미설정이면 단일 DS로 동작
            HikariDataSource single = buildDs(baseCfg, baseUrl, baseUser, basePass);
            targets.put("s0", single);
            rds.setTargetDataSources(targets);
            rds.setDefaultTargetDataSource(single);
        }

        rds.afterPropertiesSet();
        return rds;
    }

    private HikariDataSource buildDs(HikariConfig base, String url, String user, String pass) {
        HikariConfig cfg = new HikariConfig();
        // 공통 풀 설정 복사 (0 값은 기본값 유지)
        if (base.getMaximumPoolSize() > 0) cfg.setMaximumPoolSize(base.getMaximumPoolSize());
        if (base.getMinimumIdle() > 0) cfg.setMinimumIdle(base.getMinimumIdle());
        if (base.getConnectionTimeout() > 0) cfg.setConnectionTimeout(base.getConnectionTimeout());
        if (base.getIdleTimeout() > 0) cfg.setIdleTimeout(base.getIdleTimeout());
        if (base.getMaxLifetime() > 0) cfg.setMaxLifetime(base.getMaxLifetime());
        if (base.getPoolName() != null) cfg.setPoolName(base.getPoolName());

        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        return new HikariDataSource(cfg);
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    // 전역 기본 DS는 Lazy 프록시 (@Primary)
    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("routingDataSource") DataSource routing) {
        return new LazyConnectionDataSourceProxy(routing);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("dataSource") DataSource dataSource) {
        var vendor = new HibernateJpaVendorAdapter();
        vendor.setGenerateDdl(false);
        vendor.setShowSql(true);

        var emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.cnwv.game_server");
        emf.setJpaVendorAdapter(vendor);
        return emf;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            LocalContainerEntityManagerFactoryBean emfBean) {
        return new JpaTransactionManager(emfBean.getObject());
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
            @Qualifier("dataSource") DataSource dataSource) {
        return props -> props.put("hibernate.hikari.dataSource", dataSource);
    }
}
