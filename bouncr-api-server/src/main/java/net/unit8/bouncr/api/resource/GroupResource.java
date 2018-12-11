package net.unit8.bouncr.api.resource;

import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.resource.AllowedMethods;

import javax.inject.Inject;

import static kotowari.restful.DecisionPoint.IS_AUTHORIZED;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class GroupResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(IS_AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }
}
