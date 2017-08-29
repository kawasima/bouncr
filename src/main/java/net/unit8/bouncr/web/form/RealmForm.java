package net.unit8.bouncr.web.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class RealmForm extends FormBase {
    private Long id;

    @NotBlank
    @Length(max = 100)
    private String name;

    @NotBlank
    @Length(max = 255)
    private String description;

    @NotBlank
    @Length(max = 255)
    private String url;
    private Long applicationId;

    private List<AssignmentForm> assignments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentForm implements Serializable {
        private Long groupId;
        private List<Long> roleId;
    }
}
