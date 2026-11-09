package no.fintlabs.assignment.entra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EntraMembershipBatchLookup {

    private final JdbcTemplate jdbcTemplate;

    public <T> List<T> findAll(Set<MembershipKey> requested, MembershipTable table, JpaRepository<T, Long> repository) {
        if (requested.isEmpty()) {
            return List.of();
        }

        String values = String.join(", ", Collections.nCopies(requested.size(), "(?, ?)"));
        String sql = """
                SELECT membership.id
                FROM (VALUES %s) AS requested(entra_id, resource_entra_id)
                JOIN %s membership
                  ON membership.%s = requested.entra_id
                 AND membership.resource_entra_id = requested.resource_entra_id
                """.formatted(values, table.tableName, table.entraIdColumn);
        List<Object> parameters = new ArrayList<>(requested.size() * 2);
        requested.forEach(key -> {
            parameters.add(key.entraId());
            parameters.add(key.groupId());
        });

        List<Long> ids = jdbcTemplate.query(sql, (resultSet, row) -> resultSet.getLong(1), parameters.toArray());
        return ids.isEmpty() ? List.of() : repository.findAllById(ids);
    }

    public record MembershipKey(UUID entraId, UUID groupId) {}

    public enum MembershipTable {
        DEVICE("device_entra_memberships", "device_entra_id"),
        USER("user_entra_memberships", "user_entra_id");

        private final String tableName;
        private final String entraIdColumn;

        MembershipTable(String tableName, String entraIdColumn) {
            this.tableName = tableName;
            this.entraIdColumn = entraIdColumn;
        }
    }
}
