package net.unit8.bouncr.api.boundary;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

public class ApplicationUpdateRequest implements Serializable {
    @NotBlank
    @Length(max = 100)
    @Pattern(regexp = "^\\w+$")
    private String name;
    @NotBlank
    private String description;

    @JsonProperty("pass_to")
    @NotBlank
    @URL
    @Length(max = 100)
    private String passTo;

    @JsonProperty("virtual_path")
    @NotBlank
    @Length(max = 100)
    private String virtualPath;

    @JsonProperty("top_page")
    @NotBlank
    @URL
    @Length(max = 100)
    private String topPage;

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
}
