package net.unit8.bouncr.api.boundary;

import java.io.Serializable;

public class ApplicationSearchParams implements Serializable {
    private String q;
    private String embed;
    private Integer limit = 10;
    private Integer offset = 0;
}
