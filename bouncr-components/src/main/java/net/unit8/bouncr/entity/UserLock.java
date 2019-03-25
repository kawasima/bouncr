package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_locks")
public class UserLock implements Serializable {
    @Id
    @OneToOne
    @JoinColumn(name ="user_id")
    private User user;

    @JsonProperty("lock_level")
    @Enumerated(EnumType.STRING)
    @Column(name = "lock_level")
    private LockLevel lockLevel;

    @JsonProperty("locked_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LockLevel getLockLevel() {
        return lockLevel;
    }

    public void setLockLevel(LockLevel lockLevel) {
        this.lockLevel = lockLevel;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }
}
