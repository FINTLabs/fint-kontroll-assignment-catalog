package no.fintlabs.user;

import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserMapperTest {
    @Test
    void shouldMapKontrollUserToUser() {
        KontrollUser kontrollUser = new KontrollUser();
        kontrollUser.setId(1L);
        kontrollUser.setUserName("testUser");
        kontrollUser.setIdentityProviderUserObjectId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        kontrollUser.setFirstName("John");
        kontrollUser.setLastName("Doe");
        kontrollUser.setUserType("Admin");
        kontrollUser.setMainOrganisationUnitId("100");
        kontrollUser.setMainOrganisationUnitName("Novari");
        kontrollUser.setStatus("Active");
        kontrollUser.setStatusChanged(Date.valueOf("2023-10-01"));

        User user = UserMapper.fromKontrollUser(kontrollUser);

        assertEquals(1L, user.getId());
        assertEquals("testUser", user.getUserName());
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), user.getIdentityProviderUserObjectId());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("Admin", user.getUserType());
        assertEquals("100", user.getOrganisationUnitId());
        assertEquals("Novari", user.getOrganisationUnitName());
        assertEquals("Active", user.getStatus());
        assertEquals(Date.valueOf("2023-10-01"), user.getStatusChanged());
    }
}
