package net.unit8.bouncr.web.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class RealmForm extends FormBase {
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 255)
    private String description;

    @NotBlank
    @Size(max = 255)
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
