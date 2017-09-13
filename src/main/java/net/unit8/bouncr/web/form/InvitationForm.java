package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Email;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class InvitationForm extends FormBase {
    @Email
    private String email;

    private List<Long> groupIds;
}
