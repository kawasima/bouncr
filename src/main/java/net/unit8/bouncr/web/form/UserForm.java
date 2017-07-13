package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

/**
 * @author kawasima
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserForm extends FormBase {
    @Length(max = 100)
    private String name;
}
