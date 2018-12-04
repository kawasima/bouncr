package net.unit8.bouncr.web.resource;

import enkan.component.BeansConverter;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.web.boundary.RoleSearchParams;
import net.unit8.bouncr.web.entity.Role;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

import static kotowari.restful.DecisionPoint.HANDLE_OK;

@AllowedMethods({"GET", "POST"})
public class RolesResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(HANDLE_OK)
    public List<Role> handleOk(RoleSearchParams params, EntityManager em) {
        return null;
    }
}
