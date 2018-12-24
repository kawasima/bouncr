package net.unit8.bouncr.api.boundary;

import javax.validation.constraints.Email;
import java.io.Serializable;
import java.util.List;

public class InvitationCreateRequest implements Serializable {
    @Email
    private String email;

    private List<IdObject> groups;

    public static class IdObject implements Serializable {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

}
