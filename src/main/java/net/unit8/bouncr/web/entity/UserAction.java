package net.unit8.bouncr.web.entity;

import org.seasar.doma.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_actions")
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getActorIp() {
        return actorIp;
    }

    public void setActorIp(String actorIp) {
        this.actorIp = actorIp;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
