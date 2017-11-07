package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.*;

import java.io.Serializable;

@Data
@Entity
@Table(name = "user_profile_values")
public class UserProfileValue implements Serializable {
    @Id
    private Long userProfileFieldId;
    @Id
    private Long userId;
    private String value;
}
