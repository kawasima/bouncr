package net.unit8.bouncr.sign;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class JwtClaim implements Serializable {
    public String email;
    public String picture;
    public String sub;
    public String iss;
    public String aud;
    public String name;
    @JsonProperty("preferred_username")
    public String preferredUsername;
    public Long exp;
    public Long iat;
    public String nonce;
}
