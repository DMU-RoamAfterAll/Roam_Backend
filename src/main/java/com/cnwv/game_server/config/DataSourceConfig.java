package com.cnwv.game_server.config;

import com.cnwv.game_server.shard.RoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
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

    /** 샤드별 실제 커넥션 풀(Hikari) — application.properties 의 app.shard.datasources.* 에서 바인딩 */
    @Bean("dsS0")
    @ConfigurationProperties("app.shard.datasources.s0")
    public HikariDataSource dsS0() {
        // 바인딩되는 키 예:
        // app.shard.datasources.s0.jdbc-url=...
        // app.shard.datasources.s0.username=...
        // app.shard.datasources.s0.password=...
        // (필요시) app.shard.datasources.s0.maximum-pool-size=...
        return new HikariDataSource();
    }

    @Bean("dsS1")
    @ConfigurationProperties("app.shard.datasources.s1")
    public HikariDataSource dsS1() {
        return new HikariDataSource();
    }

    /**
     * 애플리케이션이 바라보는 단 하나의 DataSource.
     * RoutingDataSource -> LazyConnectionDataSourceProxy 순서로 감싸서
     * 실제 커넥션 픽업이 트랜잭션(커밋 시점) 직전에 일어나게 함.
     */
    @Bean(name = "dataSource")
    @Primary
    public DataSource dataSource(
            @Qualifier("dsS0") DataSource s0,
            @Qualifier("dsS1") DataSource s1
    ) {
        Map<Object, Object> targets = new HashMap<>();
        targets.put("s0", s0);
        targets.put("s1", s1);

        RoutingDataSource routing = new RoutingDataSource();
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(s0); // 오동작 시 메타DB가 아니라 s0으로만 가도록 안전장치
        routing.afterPropertiesSet();

        return new LazyConnectionDataSourceProxy(routing);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("dataSource") DataSource dataSource,
            org.springframework.core.env.Environment env) {

        var vendor = new HibernateJpaVendorAdapter();
        vendor.setGenerateDdl(false);
        vendor.setShowSql(true);

        var props = new java.util.HashMap<String, Object>();
        // application.properties 값을 반영 (없으면 기본값)
        props.put("hibernate.hbm2ddl.auto",
                env.getProperty("spring.jpa.hibernate.ddl-auto", "none"));
        props.put("hibernate.show_sql",
                env.getProperty("spring.jpa.show-sql", "false"));
        // 너 설정과 일치: MariaDBDialect
        var dialect = env.getProperty("spring.jpa.properties.hibernate.dialect");
        if (dialect != null && !dialect.isBlank()) {
            props.put("hibernate.dialect", dialect);
        }

        var emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.cnwv.game_server");
        emf.setJpaVendorAdapter(vendor);
        emf.setJpaPropertyMap(props);
        return emf;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean emfBean) {
        return new JpaTransactionManager(emfBean.getObject());
    }
}
