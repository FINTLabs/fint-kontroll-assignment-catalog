package no.fintlabs.assignment.user.entra;

import no.fintlabs.entra.membership.EntraReturnCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntraUserGroupMembershipResultTest {
    @Test
    public void serializationTest() throws Exception {
        EntraUserGroupMembershipResult original = new EntraUserGroupMembershipResult(EntraReturnCode.ADDED, UUID.randomUUID(), UUID.randomUUID());

        ObjectMapper objectMapper = new ObjectMapper();

        String json = objectMapper.writeValueAsString(original);

        EntraUserGroupMembershipResult deserialized = objectMapper.readValue(json, EntraUserGroupMembershipResult.class);

        assertEquals(original.getCode(), deserialized.getCode());
        assertEquals(original.getEntraGroupRef(), deserialized.getEntraGroupRef());
        assertEquals(original.getEntraUserRef(), deserialized.getEntraUserRef());
    }

    @Test
    public void shouldDeserializeFromJson() throws Exception {
        String json = "{\"code\":\"ADDED\",\"entraGroupRef\":\"123e4567-e89b-12d3-a456-426614174000\",\"entraUserRef\":\"123e4567-e89b-12d3-a456-426614174000\"}";

        ObjectMapper objectMapper = new ObjectMapper();

        EntraUserGroupMembershipResult deserialized = objectMapper.readValue(json, EntraUserGroupMembershipResult.class);

        assertEquals(EntraReturnCode.ADDED, deserialized.getCode());
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), deserialized.getEntraGroupRef());
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), deserialized.getEntraUserRef());
    }
}
