package no.fintlabs.groupmembership;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntraIdGroupMembershipTest {
    @Test
    public void serializationTest() throws Exception {
        EntraIdGroupMembership original = new EntraIdGroupMembership(EntraStatus.ADDED, UUID.randomUUID(), UUID.randomUUID());

        ObjectMapper objectMapper = new ObjectMapper();

        String json = objectMapper.writeValueAsString(original);

        EntraIdGroupMembership deserialized = objectMapper.readValue(json, EntraIdGroupMembership.class);

        assertEquals(original.getCode(), deserialized.getCode());
        assertEquals(original.getEntraGroupRef(), deserialized.getEntraGroupRef());
        assertEquals(original.getEntraUserRef(), deserialized.getEntraUserRef());
    }

    @Test
    public void shouldDeserializeFromJson() throws Exception {
        String json = "{\"code\":\"ADDED\",\"entraGroupRef\":\"123e4567-e89b-12d3-a456-426614174000\",\"entraUserRef\":\"123e4567-e89b-12d3-a456-426614174000\"}";

        ObjectMapper objectMapper = new ObjectMapper();

        EntraIdGroupMembership deserialized = objectMapper.readValue(json, EntraIdGroupMembership.class);

        assertEquals(EntraStatus.ADDED, deserialized.getCode());
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), deserialized.getEntraGroupRef());
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), deserialized.getEntraUserRef());
    }
}
