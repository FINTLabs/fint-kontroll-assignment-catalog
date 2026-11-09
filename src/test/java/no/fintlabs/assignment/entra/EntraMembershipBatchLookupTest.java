package no.fintlabs.assignment.entra;

import no.fintlabs.assignment.device.entra.DeviceEntraMembership;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.assignment.user.entra.UserEntraMembership;
import no.fintlabs.assignment.user.entra.UserEntraMembershipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EntraMembershipBatchLookupTest {

    @Test
    void deviceLookupMatchesOnlyRequestedPairs() {
        DeviceEntraMembershipRepository repository = mock(DeviceEntraMembershipRepository.class);
        when(repository.findAllById(any()))
                .thenReturn(List.of(new DeviceEntraMembership(), new DeviceEntraMembership()));
        verifyExactPairs("device", "device_entra_memberships", "device_entra_id",
                EntraMembershipBatchLookup.MembershipTable.DEVICE, repository);
    }

    @Test
    void userLookupMatchesOnlyRequestedPairs() {
        UserEntraMembershipRepository repository = mock(UserEntraMembershipRepository.class);
        when(repository.findAllById(any()))
                .thenReturn(List.of(new UserEntraMembership(), new UserEntraMembership()));
        verifyExactPairs("user", "user_entra_memberships", "user_entra_id",
                EntraMembershipBatchLookup.MembershipTable.USER, repository);
    }

    private <T> void verifyExactPairs(String database, String tableName, String idColumn,
                                      EntraMembershipBatchLookup.MembershipTable table,
                                      JpaRepository<T, Long> repository) {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:membership-pairs-" + database + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE " + tableName + " (id BIGINT PRIMARY KEY, " + idColumn
                + " UUID NOT NULL, resource_entra_id UUID NOT NULL, UNIQUE (" + idColumn
                + ", resource_entra_id))");
        UUID member1 = UUID.randomUUID();
        UUID member2 = UUID.randomUUID();
        UUID group1 = UUID.randomUUID();
        UUID group2 = UUID.randomUUID();
        jdbc.update("INSERT INTO " + tableName + " VALUES (?, ?, ?)", 1L, member1, group1);
        jdbc.update("INSERT INTO " + tableName + " VALUES (?, ?, ?)", 2L, member2, group2);
        jdbc.update("INSERT INTO " + tableName + " VALUES (?, ?, ?)", 3L, member1, group2);

        EntraMembershipBatchLookup lookup = new EntraMembershipBatchLookup(jdbc);
        lookup.findAll(new LinkedHashSet<>(Set.of(
                new EntraMembershipBatchLookup.MembershipKey(member1, group1),
                new EntraMembershipBatchLookup.MembershipKey(member2, group2))), table, repository);

        verify(repository).findAllById(argThat(ids -> {
            List<Long> selected = StreamSupport.stream(ids.spliterator(), false).toList();
            assertThat(selected).containsExactlyInAnyOrder(1L, 2L);
            return true;
        }));
    }
}
