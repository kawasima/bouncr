package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.io.Serializable;

@Data
@Entity
@Table(name = "memberships")
public class Membership implements Serializable {
    @Id
    private Long userId;

    @Id
    private Long groupId;
}
