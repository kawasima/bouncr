package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.data.AssignmentRef;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.raoh.Ok;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the AssignmentsRequest JSON decoding.
 * The old boundary class AssignmentsRequest has been replaced by raoh-json decoders.
 */
class AssignmentsRequestTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void decodeAssignments() {
        String json = """
                [
                  {
                    "group": {"id": 1, "name": "admin"},
                    "role":  {"id": 2, "name": "admin_role"},
                    "realm": {"id": 3, "name": "BOUNCR"}
                  }
                ]
                """;
        JsonNode node = mapper.readTree(json);
        var result = BouncrJsonDecoders.ASSIGNMENTS.decode(node);
        assertThat(result).isInstanceOf(Ok.class);

        @SuppressWarnings("unchecked")
        List<AssignmentRef> items = ((Ok<List<AssignmentRef>>) result).value();
        assertThat(items).hasSize(1);
        assertThat(items.getFirst().group().id()).isEqualTo(1L);
        assertThat(items.getFirst().group().name()).isEqualTo("admin");
        assertThat(items.getFirst().role().id()).isEqualTo(2L);
        assertThat(items.getFirst().realm().id()).isEqualTo(3L);
    }

    @Test
    void decodeAssignmentsWithNameOnly() {
        String json = """
                [
                  {
                    "group": {"name": "admin"},
                    "role":  {"name": "admin_role"},
                    "realm": {"name": "BOUNCR"}
                  }
                ]
                """;
        JsonNode node = mapper.readTree(json);
        var result = BouncrJsonDecoders.ASSIGNMENTS.decode(node);
        assertThat(result).isInstanceOf(Ok.class);

        @SuppressWarnings("unchecked")
        List<AssignmentRef> items = ((Ok<List<AssignmentRef>>) result).value();
        assertThat(items).hasSize(1);
        assertThat(items.getFirst().group().id()).isNull();
        assertThat(items.getFirst().group().name()).isEqualTo("admin");
    }
}
