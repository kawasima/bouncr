package net.unit8.bouncr.api.boundary;

import java.io.Serializable;
import java.util.List;

public class GroupUsersCreateRequest implements Serializable {
    private List<String> users;

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }
}
