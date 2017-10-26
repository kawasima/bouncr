package net.unit8.bouncr.web.controller.data;

import lombok.Data;

import java.io.Serializable;

@Data
public class AccessToken implements Serializable {
    private boolean active = true;
    private String scope;
    private String sub;
    private String clientId;
}
