package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.seasar.doma.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_actions")
@Data
@EqualsAndHashCode
public class UserAction implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_action_id")
    private Long id;

    @Column(name = "action_id")
    ActionType actionType;
    private String actor;
    private String actorIp;
    private String options;
    private LocalDateTime createdAt;
}
