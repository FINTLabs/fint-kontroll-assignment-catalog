package no.fintlabs.role;

import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Date;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RoleConsumerTest {
    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EntityConsumerFactoryService factoryService;

    @Mock
    private FintCache<Long, Role> roleCache;

    @Mock
    private AssignmentService assignmentService;

    private RoleConsumer consumer;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        consumer = new RoleConsumer(roleRepository, assignmentService, roleCache);
    }

    @Test
    public void processRole_handlesUpdate() {
        String key = "dummykey";

        Role updatedRole = Role.builder()
                .id(1L)
                .roleName("roleName")
                .build();

        Role existingRole = Role.builder()
                .id(1L)
                .roleName("roleName_changed")
                .build();

        ConsumerRecord<String, Role> record = new ConsumerRecord<>("topic", 1, 1, key, updatedRole);

        when(roleRepository.findById(updatedRole.getId())).thenReturn(java.util.Optional.of(existingRole));

        consumer.process(record);

        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        verify(roleCache, times(1)).put(updatedRole.getId(), updatedRole);

    }

    @Test
    public void processRole_cached_equal() {
        String key = "dummykey";

        Role updatedRole = Role.builder()
                .id(1L)
                .roleName("roleName")
                .build();

        Role existingRole = Role.builder()
                .id(1L)
                .roleName("roleName")
                .build();

        ConsumerRecord<String, Role> record = new ConsumerRecord<>("topic", 1, 1, key, updatedRole);

        when(roleCache.getOptional(updatedRole.getId())).thenReturn(java.util.Optional.of(existingRole));

        consumer.process(record);

        verify(roleRepository, times(0)).findById(updatedRole.getId());
        verify(roleRepository, times(0)).save(updatedRole);
        verify(roleCache, times(0)).put(updatedRole.getId(), updatedRole);
    }

    @Test
    public void processRole_cached_changed() {
        String key = "dummykey";

        Role updatedRole = Role.builder()
                .id(1L)
                .roleName("roleName_changed")
                .build();

        Role existingRole = Role.builder()
                .id(1L)
                .roleName("roleName")
                .build();

        ConsumerRecord<String, Role> record = new ConsumerRecord<>("topic", 1, 1, key, updatedRole);

        when(roleCache.getOptional(updatedRole.getId())).thenReturn(java.util.Optional.of(existingRole));

        consumer.process(record);

        verify(roleRepository, times(1)).findById(updatedRole.getId());
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        verify(roleCache, times(1)).put(updatedRole.getId(), updatedRole);
    }

    @Test
    public void processRole_deactivate() {
        Role updatedRole = Role.builder()
                .id(1L)
                .roleName("roleName")
                .roleStatus("Inactive")
                .build();

        Role existingRole = Role.builder()
                .id(1L)
                .roleName("roleName_changed")
                .roleStatus("Active")
                .roleStatusChanged(Date.from(Instant.parse("2023-08-01T12:00:00Z")))
                .build();

        when(roleRepository.findById(updatedRole.getId())).thenReturn(java.util.Optional.of(existingRole));

        consumer.processRoleUpdate(updatedRole);

        verify(assignmentService, times(1)).activateOrDeactivateAssignmentsByRole(updatedRole);
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        verify(roleCache, times(1)).put(updatedRole.getId(), updatedRole);
    }

    @Test
    public void processRole_activate() {
        Role updatedRole = Role.builder()
                .id(1L)
                .roleName("roleName")
                .roleStatus("active")
                .build();

        Role existingRole = Role.builder()
                .id(1L)
                .roleName("roleName_changed")
                .roleStatus("Inactive")
                .roleStatusChanged(Date.from(Instant.parse("2023-08-01T12:00:00Z")))
                .build();

        when(roleRepository.findById(updatedRole.getId())).thenReturn(java.util.Optional.of(existingRole));

        consumer.processRoleUpdate(updatedRole);

        verify(assignmentService, times(1)).activateOrDeactivateAssignmentsByRole(updatedRole);
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        verify(roleCache, times(1)).put(updatedRole.getId(), updatedRole);
    }
}
