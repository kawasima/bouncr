package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author kawasima
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PermissionForm extends FormBase {
    private Long id;

    @NotBlank
    @Length(max = 100)
    private String name;

    @NotBlank
    @Length(max = 255)
    private String description;
}
