package no.fintlabs.user;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserTest {
    @Test
    void hasSamePersistedStateAsShouldReturnTrueForSameObject() {
        User user = new User();
        assertTrue(user.hasSamePersistedStateAs(user));
    }

    @Test
    void hasSamePersistedStateAsShouldReturnFalseForNull() {
        User user = new User();
        assertFalse(user.hasSamePersistedStateAs(null));
    }

    @Test
    void hasSamePersistedStateAsShouldReturnTrueForEqualUsers() {
        User user1 = User.builder()
                .id(1L)
                .userName("testUser")
                .identityProviderUserObjectId(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .userType("admin")
                .organisationUnitId("org1")
                .organisationUnitName("Org Unit 1")
                .status("active")
                .statusChanged(new Date())
                .build();

        User user2 = User.builder()
                .id(1L)
                .userName("testUser")
                .identityProviderUserObjectId(user1.getIdentityProviderUserObjectId())
                .firstName("John")
                .lastName("Doe")
                .userType("admin")
                .organisationUnitId("org1")
                .organisationUnitName("Org Unit 1")
                .status("active")
                .statusChanged(user1.getStatusChanged())
                .build();

        assertTrue(user1.hasSamePersistedStateAs(user2));
    }

    @Test
    void hasSamePersistedStateAsShouldReturnFalseForDifferentUsers() {
        User user1 = User.builder()
                .id(1L)
                .userName("testUser1")
                .identityProviderUserObjectId(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .userType("admin")
                .organisationUnitId("org1")
                .organisationUnitName("Org Unit 1")
                .status("active")
                .statusChanged(new Date())
                .build();

        User user2 = User.builder()
                .id(2L)
                .userName("testUser2")
                .identityProviderUserObjectId(UUID.randomUUID())
                .firstName("Jane")
                .lastName("Smith")
                .userType("user")
                .organisationUnitId("org2")
                .organisationUnitName("Org Unit 2")
                .status("inactive")
                .statusChanged(new Date())
                .build();

        assertFalse(user1.hasSamePersistedStateAs(user2));
    }
}
