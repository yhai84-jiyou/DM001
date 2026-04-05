package com.helpmanual.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
public class DataSourceConfig {

    @Bean
    public CommandLineRunner sqlitePragmaRunner(DataSource dataSource) {
        return args -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA busy_timeout=5000;");
                stmt.execute("CREATE VIRTUAL TABLE IF NOT EXISTS article_fts USING fts5(title, content, article_id UNINDEXED);");
            }
        };
    }
}
