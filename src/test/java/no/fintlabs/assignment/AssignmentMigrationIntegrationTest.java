package no.fintlabs.assignment;

import no.fintlabs.DatabaseIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class AssignmentMigrationIntegrationTest extends DatabaseIntegrationTest {

    @Test
    void upgradesDeployedV33AndBackfillsActionableAssignmentsBeforeHistory() {
        Flyway deployed = Flyway.configure()
                .dataSource(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword())
                .locations("classpath:db/migration")
                .target("33")
                .load();
        deployed.migrate();
        assertThat(deployed.info().current().getVersion().toString()).isEqualTo("33");

        JdbcTemplate jdbc = new JdbcTemplate(deployed.getConfiguration().getDataSource());
        UUID groupId = UUID.fromString("20000000-0000-0000-0000-000000000001");
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        jdbc.update("INSERT INTO assignment_resources (id, status, identity_provider_group_object_id) VALUES (1, 'ACTIVE', ?)", groupId);
        jdbc.update("INSERT INTO assignment_resources (id, status) VALUES (999, 'ACTIVE')");
        jdbc.update("INSERT INTO users (id, identity_provider_user_object_id, status) VALUES (999, ?, NULL)",
                UUID.fromString("10000000-0000-0000-0000-000000000999"));
        jdbc.update("INSERT INTO assignments (id, resource_ref, azure_ad_group_id, azure_ad_user_id) VALUES (1, 1, ?, ?)", groupId, userId);
        jdbc.update("""
                INSERT INTO flattened_assignments
                    (assignment_id, identity_provider_user_object_id, identity_provider_group_object_id,
                     identity_provider_group_membership_confirmed, identity_provider_group_membership_deletion_confirmed,
                     resource_ref, assignment_termination_date)
                VALUES
                    (1, '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', true, false, 1, NULL),
                    (2, '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', false, false, 1, NULL),
                    (3, '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', true, true, 1, now()),
                    (1, '10000000-0000-0000-0000-000000000003', NULL, false, false, 1, NULL),
                    (4, '10000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001', true, false, 1, now())
                """);
        Flyway upgraded = Flyway.configure()
                .dataSource(deployed.getConfiguration().getDataSource())
                .locations("classpath:db/migration")
                .target("39")
                .load();
        assertThat(upgraded.migrate().migrationsExecuted).isEqualTo(6);
        upgraded.validate();
        assertThat(upgraded.info().current().getVersion().toString()).isEqualTo("39");

        assertThat(jdbc.queryForObject("SELECT entra_group_id FROM assignments WHERE id = 1", UUID.class)).isEqualTo(groupId);
        assertThat(jdbc.queryForObject("SELECT entra_user_id FROM assignments WHERE id = 1", UUID.class)).isEqualTo(userId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_entra_memberships", Long.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'flattened_user_assignments' AND column_name = 'assigner_ref'
                """, Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flattened_user_assignments WHERE user_entra_membership_id IS NULL", Long.class))
                .isEqualTo(5L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flattened_user_assignments WHERE identity_provider_group_object_id IS NULL", Long.class))
                .isZero();

        new ResourceDatabasePopulator(new ClassPathResource(
                "db/manual/migrate_flattened_assignments_to_user_entra_memberships.sql"
        )).execute(jdbc.getDataSource());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_entra_memberships", Long.class)).isEqualTo(3L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM assignment_resources WHERE id = 999", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM users WHERE id = 999", String.class)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_entra_memberships WHERE membership_status = 'ACTIVE'", Long.class))
                .isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT entra_status FROM user_entra_memberships WHERE user_entra_id = ?", String.class, userId))
                .isEqualTo("NOT_SENT");
        assertThat(jdbc.queryForObject("SELECT entra_status FROM user_entra_memberships WHERE membership_status = 'INACTIVE'", String.class))
                .isEqualTo("TO_BE_DELETED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flattened_user_assignments WHERE user_entra_membership_id IS NOT NULL", Long.class))
                .isEqualTo(4L);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM flattened_user_assignments
                WHERE user_entra_membership_id IS NULL AND assignment_termination_date IS NOT NULL
                  AND identity_provider_group_membership_deletion_confirmed IS TRUE
                """, Long.class)).isEqualTo(1L);

        new ResourceDatabasePopulator(new ClassPathResource(
                "db/manual/complete_flattened_assignments_to_user_entra_memberships.sql"
        )).execute(jdbc.getDataSource());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_entra_memberships", Long.class)).isEqualTo(4L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_entra_memberships WHERE entra_status = 'DELETION_CONFIRMED'", Long.class))
                .isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flattened_user_assignments WHERE user_entra_membership_id IS NOT NULL", Long.class))
                .isEqualTo(5L);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(DISTINCT user_entra_membership_id)
                FROM flattened_user_assignments
                WHERE assignment_termination_date IS NULL
                """, Long.class)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name IN ('flattened_user_assignments', 'flattened_device_assignments')
                  AND column_name IN ('identity_provider_user_object_id', 'identity_provider_device_object_id', 'identity_provider_group_object_id')
                """, Long.class)).isZero();
    }
}
