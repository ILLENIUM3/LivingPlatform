package com.liveplatform.user.provider.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.junit.jupiter.api.AfterEach;
import org.springframework.dao.DataAccessException;

import static org.assertj.core.api.Assertions.*;

class DatabaseProbeServiceTest {
    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;
    private DatabaseProbeService service;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2).build();
        jdbc = new JdbcTemplate(database);
        jdbc.execute("CREATE TABLE platform_bootstrap (id INT PRIMARY KEY, probe_value VARCHAR(64))");
        jdbc.update("INSERT INTO platform_bootstrap VALUES (1, 'first-value')");
        service = new DatabaseProbeService(jdbc);
    }

    @AfterEach
    void tearDown() { database.shutdown(); }

    @Test
    void readsDatabaseOnEveryCallRatherThanReturningACachedConstant() {
        assertThat(service.probe().getProbeValue()).isEqualTo("first-value");
        jdbc.update("UPDATE platform_bootstrap SET probe_value = 'changed' WHERE id = 1");
        var result = service.probe();
        assertThat(result.getProbeValue()).isEqualTo("changed");
        assertThat(result.getDatabaseProduct()).isEqualTo("H2");
        assertThat(result.getDatabase()).isNotBlank();
    }

    @Test
    void missingSeedDoesNotReportSuccess() {
        jdbc.update("DELETE FROM platform_bootstrap");
        assertThatThrownBy(service::probe).isInstanceOf(DataAccessException.class);
    }

    @Test
    void missingSchemaDoesNotReportSuccess() {
        jdbc.execute("DROP TABLE platform_bootstrap");
        assertThatThrownBy(service::probe).isInstanceOf(DataAccessException.class);
    }
}
