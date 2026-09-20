package com.pulsepass;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FlywayMigrationIT {

    @Autowired
    private Flyway flyway;

    @Autowired
    private Environment env;

    @Test
    void flywayAppliesV1V2V3() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).hasSize(3);
        assertThat(applied[0].getVersion().getVersion()).isEqualTo("1");
        assertThat(applied[1].getVersion().getVersion()).isEqualTo("2");
        assertThat(applied[2].getVersion().getVersion()).isEqualTo("3");
    }

    @Test
    void hibernateValidatesSchema() {
        assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }
}
