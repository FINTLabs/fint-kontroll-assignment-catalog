package no.fintlabs.resource;

import no.fintlabs.assignment.exception.AssignmentException;
import no.fintlabs.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceAssignmentValidatorTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceAssignmentValidator resourceAssignmentValidator;

    @Test
    void returnsActiveResourceWithEntraGroupId() {
        Resource resource = Resource.builder()
                .id(1L)
                .status("ACTIVE")
                .identityProviderGroupObjectId(UUID.randomUUID())
                .build();
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

        assertEquals(resource, resourceAssignmentValidator.getActiveResourceOrThrow(1L));
    }

    @Test
    void throwsWhenResourceIsMissing() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resourceAssignmentValidator.getActiveResourceOrThrow(1L));
    }

    @Test
    void throwsWhenResourceIsInactive() {
        Resource resource = Resource.builder()
                .id(1L)
                .status("INACTIVE")
                .identityProviderGroupObjectId(UUID.randomUUID())
                .build();
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

        AssignmentException exception = assertThrows(
                AssignmentException.class,
                () -> resourceAssignmentValidator.getActiveResourceOrThrow(1L)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("Resource 1 is not active", exception.getMessage());
    }

    @Test
    void throwsWhenResourceHasNoEntraGroupId() {
        Resource resource = Resource.builder()
                .id(1L)
                .status("ACTIVE")
                .build();
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

        AssignmentException exception = assertThrows(
                AssignmentException.class,
                () -> resourceAssignmentValidator.getActiveResourceOrThrow(1L)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("Resource 1 does not have entra group id set", exception.getMessage());
    }
}
