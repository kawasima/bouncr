package net.unit8.bouncr.web.form;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author kawasima
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ApplicationForm extends FormBase {
    private Long id;
    private String name;
}
