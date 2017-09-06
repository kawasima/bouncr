package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.io.Serializable;

@Entity
@Table(name = "memberships")
@Data
@EqualsAndHashCode
public class Membership implements Serializable {
    @Id
    private Long userId;

    @Id
    private Long groupId;
}
