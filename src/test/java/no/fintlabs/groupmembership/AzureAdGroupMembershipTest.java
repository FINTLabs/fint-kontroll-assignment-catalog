package no.fintlabs.groupmembership;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AzureAdGroupMembershipTest {
    @Test
    public void serializationTest() throws Exception {
        // Create an instance of AzureAdGroupMembership
        AzureAdGroupMembership original = new AzureAdGroupMembership("testId", UUID.randomUUID(), UUID.randomUUID());

        // Create an ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        // Serialize the AzureAdGroupMembership instance to JSON
        String json = objectMapper.writeValueAsString(original);

        // Deserialize the JSON back to an AzureAdGroupMembership instance
        AzureAdGroupMembership deserialized = objectMapper.readValue(json, AzureAdGroupMembership.class);

        // Assert that the original and deserialized AzureAdGroupMembership instances are equal
        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getAzureGroupRef(), deserialized.getAzureGroupRef());
        assertEquals(original.getAzureUserRef(), deserialized.getAzureUserRef());
    }

    @Test
    public void shouldDeserializeFromJson() throws Exception {
        // Define a JSON string with valid UUIDs
        String json = "{\"id\":\"testId\",\"group_id\":\"123e4567-e89b-12d3-a456-426614174000\",\"user_id\":\"123e4567-e89b-12d3-a456-426614174000\"}";

        // Create an ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        // Deserialize the JSON string to an AzureAdGroupMembership instance
        AzureAdGroupMembership deserialized = objectMapper.readValue(json, AzureAdGroupMembership.class);

        // Assert that the properties of the deserialized object match the expected values
        assertEquals("testId", deserialized.getId());
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), deserialized.getAzureGroupRef());
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), deserialized.getAzureUserRef());
    }
}
