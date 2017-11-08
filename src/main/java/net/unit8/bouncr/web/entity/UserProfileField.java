package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.*;

import java.io.Serializable;

@Data
@Entity
@Table(name = "user_profile_fields")
public class UserProfileField implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_profile_field_id")
    private Long id;

    private String name;
    private String jsonName;
    private boolean isRequired;
    private boolean isIdentity;

    private String regularExpression;
    private Integer maxLength;
    private Integer minLength;

    private Integer position;
}
