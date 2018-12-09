package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "user_profile_fields")
public class UserProfileField implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_profile_field_id")
    private Long id;

    private String name;

    @JsonProperty("json_name")
    @Column(name = "json_name")
    private String jsonName;

    @JsonProperty("is_required")
    @Column(name = "is_required")
    private boolean isRequired;

    @JsonProperty("is_identity")
    @Column(name = "is_identity")
    private boolean isIdentity;

    @JsonProperty("regular_expression")
    @Column(name = "regular_expression")
    private String regularExpression;

    @JsonProperty("max_length")
    @Column(name = "max_length")
    private Integer maxLength;

    @JsonProperty("min_length")
    @Column(name = "min_length")
    private Integer minLength;

    private Integer position;

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
    }

    public String getJsonName() {
        return jsonName;
    }

    public void setJsonName(String jsonName) {
        this.jsonName = jsonName;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public boolean isIdentity() {
        return isIdentity;
    }

    public void setIdentity(boolean identity) {
        isIdentity = identity;
    }

    public String getRegularExpression() {
        return regularExpression;
    }

    public void setRegularExpression(String regularExpression) {
        this.regularExpression = regularExpression;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}
