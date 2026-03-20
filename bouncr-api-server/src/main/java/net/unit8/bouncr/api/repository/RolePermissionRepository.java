package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.Permission;
import org.jooq.DSLContext;

import java.util.List;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class RolePermissionRepository {
    private final DSLContext dsl;

    public RolePermissionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<Permission> findPermissionsByRole(String roleName) {
        return dsl.select(
                        field("p.permission_id", Long.class).as("permission_id"),
                        field("p.name", String.class).as("name"),
                        field("p.description", String.class).as("description"),
                        field("p.write_protected", Boolean.class).as("write_protected"))
                .from(table("permissions").as("p"))
                .join(table("role_permissions").as("rp")).on(field("rp.permission_id").eq(field("p.permission_id")))
                .join(table("roles").as("r")).on(field("r.role_id").eq(field("rp.role_id")))
                .where(field("r.name").eq(roleName))
                .fetch(rec -> PERMISSION.decode(rec).getOrThrow());
    }

    public void addPermission(Long roleId, Long permissionId) {
        dsl.insertInto(table("role_permissions"),
                        field("role_id"), field("permission_id"))
                .values(roleId, permissionId)
                .execute();
    }

    public void removePermission(Long roleId, Long permissionId) {
        dsl.deleteFrom(table("role_permissions"))
                .where(field("role_id").eq(roleId)
                        .and(field("permission_id").eq(permissionId)))
                .execute();
    }

    public void replacePermissions(Long roleId, List<Long> permissionIds) {
        dsl.deleteFrom(table("role_permissions"))
                .where(field("role_id").eq(roleId))
                .execute();

        for (Long permissionId : permissionIds) {
            dsl.insertInto(table("role_permissions"),
                            field("role_id"), field("permission_id"))
                    .values(roleId, permissionId)
                    .execute();
        }
    }
}
