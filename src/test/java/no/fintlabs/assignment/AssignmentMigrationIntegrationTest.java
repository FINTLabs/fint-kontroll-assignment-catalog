package no.fintlabs.assignment;

import no.fintlabs.DatabaseIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class AssignmentMigrationIntegrationTest extends DatabaseIntegrationTest {

    @Test
    void upgradesDeployedV38AndCombinesAssignmentsIntoDistinctMemberships() {
        Flyway deployed = Flyway.configure()
                .dataSource(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword())
                .locations("classpath:db/migration")
                .target("38")
                .load();
        deployed.migrate();
        assertThat(deployed.info().current().getVersion().toString()).isEqualTo("38");

        JdbcTemplate jdbc = new JdbcTemplate(deployed.getConfiguration().getDataSource());
        UUID groupId = UUID.fromString("20000000-0000-0000-0000-000000000001");
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        jdbc.update("INSERT INTO assignment_resources (id, status) VALUES (999, 'ACTIVE')");
        jdbc.update("INSERT INTO assignments (id, azure_ad_group_id, azure_ad_user_id) VALUES (1, ?, ?)", groupId, userId);
        jdbc.update("""
                INSERT INTO flattened_assignments
                    (assignment_id, identity_provider_user_object_id, identity_provider_group_object_id,
                     identity_provider_group_membership_confirmed, identity_provider_group_membership_deletion_confirmed,
                     assignment_termination_date)
                VALUES
                    (1, '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', true, false, NULL),
                    (2, '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', false, false, NULL),
                    (3, '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', true, true, now())
                """);
        jdbc.update("""
                INSERT INTO device_entra_memberships
                    (device_entra_id, resource_entra_id, created_date, membership_status, entra_status)
                VALUES ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', now(), 'ACTIVE', 'SENT')
                """);

        Flyway upgraded = Flyway.configure()
                .dataSource(deployed.getConfiguration().getDataSource())
                .locations("classpath:db/migration")
                .load();
        assertThat(upgraded.migrate().migrationsExecuted).isEqualTo(2);
        upgraded.validate();
        assertThat(upgraded.info().current().getVersion().toString()).isEqualTo("40");

        assertThat(jdbc.queryForObject("SELECT entra_group_id FROM assignments WHERE id = 1", UUID.class)).isEqualTo(groupId);
        assertThat(jdbc.queryForObject("SELECT entra_user_id FROM assignments WHERE id = 1", UUID.class)).isEqualTo(userId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_entra_memberships", Long.class)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM assignment_resources WHERE id = 999", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_entra_memberships WHERE membership_status = 'ACTIVE'", Long.class))
                .isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT entra_status FROM user_entra_memberships WHERE user_entra_id = ?", String.class, userId))
                .isEqualTo("NOT_SENT");
        assertThat(jdbc.queryForObject("SELECT entra_status FROM user_entra_memberships WHERE membership_status = 'INACTIVE'", String.class))
                .isEqualTo("DELETION_CONFIRMED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flattened_user_assignments WHERE user_entra_membership_id IS NOT NULL", Long.class))
                .isEqualTo(3L);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(DISTINCT user_entra_membership_id)
                FROM flattened_user_assignments
                WHERE assignment_termination_date IS NULL
                """, Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM device_entra_memberships WHERE membership_status = 'ACTIVE' AND entra_status = 'SENT'", Long.class))
                .isEqualTo(1L);
    }
}
