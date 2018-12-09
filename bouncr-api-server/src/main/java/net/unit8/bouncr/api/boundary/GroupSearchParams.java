package net.unit8.bouncr.api.boundary;

import java.io.Serializable;

public class GroupSearchParams implements Serializable {
    private String q;
    private String embed;
    private Integer limit = 10;
    private Integer offset = 0;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getEmbed() {
        return embed;
    }

    public void setEmbed(String embed) {
        this.embed = embed;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }
}
