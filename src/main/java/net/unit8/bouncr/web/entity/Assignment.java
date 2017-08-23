package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.io.Serializable;

@Data
@Entity
@Table(name = "assignments")
public class Assignment implements Serializable {
    @Id
    private Long groupId;

    @Id
    private Long roleId;

    @Id
    private Long realmId;
}
