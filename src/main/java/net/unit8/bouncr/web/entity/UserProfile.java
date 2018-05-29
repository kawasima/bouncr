package net.unit8.bouncr.web.entity;

import org.seasar.doma.Entity;

import java.io.Serializable;

@Entity
public class UserProfile implements Serializable {
    private String name;
    private String jsonName;
    private String value;

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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
