package net.unit8.bouncr.web.entity;

import lombok.Data;
import net.unit8.bouncr.web.EventDateTime;
import net.unit8.bouncr.web.EventDateTimeEntityListener;
import org.seasar.doma.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity(listener = EventDateTimeEntityListener.class)
@Table(name = "invitations")
@Data
public class Invitation implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long id;
    private String code;

    private String email;

    @EventDateTime
    private LocalDateTime invitedAt;
}
