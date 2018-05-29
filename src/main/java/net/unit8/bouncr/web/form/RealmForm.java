package net.unit8.bouncr.web.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

public class RealmForm extends FormBase {
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 255)
    private String description;

    @NotBlank
    @Size(max = 255)
    private String url;
    private Long applicationId;

    private List<AssignmentForm> assignments;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public List<AssignmentForm> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<AssignmentForm> assignments) {
        this.assignments = assignments;
    }

    public static class AssignmentForm implements Serializable {
        private Long groupId;
        private List<Long> roleId;

        public AssignmentForm() {

        }

        public AssignmentForm(Long groupId, List<Long> roleId) {
            this.groupId = groupId;
            this.roleId = roleId;
        }

        public Long getGroupId() {
            return groupId;
        }

        public void setGroupId(Long groupId) {
            this.groupId = groupId;
        }

        public List<Long> getRoleId() {
            return roleId;
        }

        public void setRoleId(List<Long> roleId) {
            this.roleId = roleId;
        }
    }
}
