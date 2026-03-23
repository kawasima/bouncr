package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.AssignmentRepository;
import net.unit8.bouncr.api.repository.RealmRepository;
import net.unit8.bouncr.data.Assignment;
import net.unit8.bouncr.data.Realm;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods("GET")
public class RealmAssignmentsResource {

    static final ContextKey<Realm> REALM = ContextKey.of(Realm.class);

    @Decision(AUTHORIZED)
    public boolean authorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(ALLOWED)
    public boolean allowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("assignments:read") || p.hasPermission("realm:read") || p.hasPermission("any_realm:read"))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        RealmRepository repo = new RealmRepository(dsl);
        Optional<Realm> realm = repo.findByApplicationAndName(params.get("name"), params.get("realmName"));
        realm.ifPresent(r -> context.put(REALM, r));
        return realm.isPresent();
    }

    @Decision(HANDLE_OK)
    public List<Assignment> list(Realm realm, DSLContext dsl) {
        AssignmentRepository repo = new AssignmentRepository(dsl);
        return repo.findByRealm(realm.id());
    }
}
