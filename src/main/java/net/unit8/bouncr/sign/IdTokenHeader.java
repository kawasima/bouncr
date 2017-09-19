package net.unit8.bouncr.sign;

import lombok.Data;

import java.io.Serializable;

@Data
public class IdTokenHeader implements Serializable {
    private String alg;
    private String kid;
}
