package net.unit8.bouncr.web.form;


import javax.validation.constraints.Email;
import java.util.List;

public class InvitationForm extends FormBase {
    @Email
    private String email;

    private List<Long> groupIds;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<Long> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<Long> groupIds) {
        this.groupIds = groupIds;
    }
}
