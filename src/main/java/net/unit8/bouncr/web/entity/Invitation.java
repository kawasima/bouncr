package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "invitation")
@Data
public class Invitation implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long id;
    private String code;

    private String email;
    private LocalDateTime invitedAt;
}
