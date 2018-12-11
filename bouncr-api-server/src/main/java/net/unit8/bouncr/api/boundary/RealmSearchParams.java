package net.unit8.bouncr.api.boundary;

import java.io.Serializable;

public class RealmSearchParams implements Serializable {
    private String q;
    private String embed;
    private Integer offset = 0;
    private Integer limit = 10;

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

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
