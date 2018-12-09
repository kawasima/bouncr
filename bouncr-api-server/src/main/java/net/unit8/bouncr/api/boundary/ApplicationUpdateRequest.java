package net.unit8.bouncr.api.boundary;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class ApplicationUpdateRequest implements Serializable {
    @NotBlank
    private String name;
    @NotBlank
    private String description;

    @JsonProperty("pass_to")
    private String passTo;

    @JsonProperty("virtual_path")
    private String virtualPath;

    @JsonProperty("top_page")
    private String topPage;

    @JsonProperty("write_protected")
    private Boolean writeProtected;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPassTo() {
        return passTo;
    }

    public void setPassTo(String passTo) {
        this.passTo = passTo;
    }

    public String getVirtualPath() {
        return virtualPath;
    }

    public void setVirtualPath(String virtualPath) {
        this.virtualPath = virtualPath;
    }

    public String getTopPage() {
        return topPage;
    }

    public void setTopPage(String topPage) {
        this.topPage = topPage;
    }

    public Boolean getWriteProtected() {
        return writeProtected;
    }

    public void setWriteProtected(Boolean writeProtected) {
        this.writeProtected = writeProtected;
    }
}
