package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.Entity;

import java.io.Serializable;

@Data
@Entity
public class UserProfile implements Serializable {
    private String name;
    private String jsonName;
    private String value;
}
