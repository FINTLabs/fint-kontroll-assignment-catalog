package no.fintlabs.groupmembership;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceGroupMembershipTest {

    @Test
    void shouldSerializeToGatewayUserMembershipContract() throws Exception {
        UUID groupRef = UUID.randomUUID();
        UUID userRef = UUID.randomUUID();
        ResourceGroupMembership membership = new ResourceGroupMembership(OperationType.ADD, groupRef, userRef);

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(membership));

        assertEquals("ADD", json.get("operation").asText());
        assertEquals(groupRef.toString(), json.get("entraGroupRef").asText());
        assertEquals(userRef.toString(), json.get("userGroupRef").asText());
    }
}
