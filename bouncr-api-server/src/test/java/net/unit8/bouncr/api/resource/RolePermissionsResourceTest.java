package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.api.boundary.RolePermissionsRequest;
import net.unit8.bouncr.entity.Permission;
import net.unit8.bouncr.entity.Role;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Arrays;
import java.util.List;

import static enkan.util.BeanBuilder.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RolePermissionsResourceTest {
    @Test
    void create() {
        TypedQuery query = mock(TypedQuery.class);
        Mockito.when(query.getResultList()).thenReturn(
                List.of(
                        builder(new Permission())
                                .set(Permission::setId, 2L)
                                .set(Permission::setName, "test2").build(),
                        builder(new Permission())
                                .set(Permission::setId, 3L)
                                .set(Permission::setName, "test3").build()
                )
        );
        final EntityManager em = MockFactory.createEntityManagerMock(query);
        final RolePermissionsResource resource = new RolePermissionsResource();
        final RolePermissionsRequest RolePermissionsRequest = new RolePermissionsRequest();
        RolePermissionsRequest.addAll(Arrays.asList("test2", "test3"));

        final List<Permission> Permissions = List.of(
                builder(new Permission())
                        .set(Permission::setId, 1L)
                        .set(Permission::setName, "test1").build(),
                builder(new Permission())
                        .set(Permission::setId, 2L)
                        .set(Permission::setName, "test2").build()
        );
        final Role role = builder(new Role())
                .set(Role::setPermissions, Permissions)
                .build();

        resource.create(RolePermissionsRequest, role, em);

        assertThat(role.getPermissions()).containsExactly(
                builder(new Permission()).set(Permission::setId, 1L)
                        .set(Permission::setName, "test1").build(),
                builder(new Permission()).set(Permission::setId, 2L)
                        .set(Permission::setName, "test2").build(),
                builder(new Permission()).set(Permission::setId, 3L)
                        .set(Permission::setName, "test3").build()
        );
    }

    /**
     * Role has Permissions(test1, test2, test3).
     * Delete test1 and test2 Permissions.
     * Then, the Role has only test2 Permission.
     */
    @Test
    void delete() {
        TypedQuery query = mock(TypedQuery.class);
        Mockito.when(query.getResultList()).thenReturn(
                List.of(
                        builder(new Permission()).set(Permission::setId, 1L).set(Permission::setName, "test1").build(),
                        builder(new Permission()).set(Permission::setId, 3L).set(Permission::setName, "test3").build()
                )
        );
        final EntityManager em = MockFactory.createEntityManagerMock(query);
        final RolePermissionsResource resource = new RolePermissionsResource();
        final RolePermissionsRequest RolePermissionsRequest = new RolePermissionsRequest();
        RolePermissionsRequest.addAll(Arrays.asList("test1", "test3"));

        final List<Permission> permissions = List.of(
                builder(new Permission()).set(Permission::setId, 1L).set(Permission::setName, "test1").build(),
                builder(new Permission()).set(Permission::setId, 2L).set(Permission::setName, "test2").build(),
                builder(new Permission()).set(Permission::setId, 3L).set(Permission::setName, "test3").build()
        );
        final Role role = builder(new Role())
                .set(Role::setPermissions, permissions)
                .build();

        resource.delete(RolePermissionsRequest, role, em);

        assertThat(role.getPermissions()).containsExactly(
                builder(new Permission()).set(Permission::setId, 2L).set(Permission::setName, "test2").build()
        );
    }

}