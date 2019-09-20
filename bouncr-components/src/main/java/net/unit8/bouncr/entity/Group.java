package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.*;
import net.unit8.bouncr.json.IndirectListFilter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * The entity of groups.
 *
 * @author kawasima
 */
@Entity
@Table(name = "groups")
public class Group implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    private String name;
    @JsonIgnore
    @Column(name = "name_lower")
    private String nameLower;
    private String description;

    @JsonIgnore
    @Column(name = "write_protected")
    private Boolean writeProtected;

    @ManyToMany
    @JoinTable(name = "memberships",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = IndirectListFilter.class)
    private List<User> users;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.nameLower = Optional.ofNullable(name)
                .map(n -> n.toLowerCase(Locale.US))
                .orElse(null);
    }

    public String getNameLower() {
        return nameLower;
    }

    public void setNameLower(String nameLower) {
        this.nameLower = nameLower;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getWriteProtected() {
        return writeProtected;
    }

    public void setWriteProtected(Boolean writeProtected) {
        this.writeProtected = writeProtected;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        return Optional.ofNullable(obj)
                .filter(o -> getClass().isInstance(o))
                .map(o -> getClass().cast(o))
                .filter(o -> getId() != null && getId().equals(o.getId()))
                .isPresent();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", writeProtected=" + writeProtected +
                '}';
    }
}
