package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.api.boundary.GroupUsersRequest;
import net.unit8.bouncr.entity.Group;
import net.unit8.bouncr.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Arrays;
import java.util.List;

import static enkan.util.BeanBuilder.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GroupUsersResourceTest {
    @Test
    void create() {
        TypedQuery query = mock(TypedQuery.class);
        Mockito.when(query.getResultList()).thenReturn(
                List.of(
                        builder(new User()).set(User::setId, 2L).set(User::setAccount, "test2").build(),
                        builder(new User()).set(User::setId, 3L).set(User::setAccount, "test3").build()
                )
        );
        final EntityManager em = MockFactory.createEntityManagerMock(query);
        final GroupUsersResource resource = new GroupUsersResource();
        final GroupUsersRequest groupUsersRequest = new GroupUsersRequest();
        groupUsersRequest.addAll(Arrays.asList("test2", "test3"));

        final List<User> users = List.of(
                builder(new User()).set(User::setId, 1L).set(User::setAccount, "test1").build(),
                builder(new User()).set(User::setId, 2L).set(User::setAccount, "test2").build()
        );
        final Group group = builder(new Group())
                .set(Group::setUsers, users)
                .build();

        resource.create(groupUsersRequest, group, em);

        assertThat(group.getUsers()).containsExactly(
                builder(new User()).set(User::setId, 1L).set(User::setAccount, "test1").build(),
                builder(new User()).set(User::setId, 2L).set(User::setAccount, "test2").build(),
                builder(new User()).set(User::setId, 3L).set(User::setAccount, "test3").build()
        );
    }

    /**
     * Group has users(test1, test2, test3).
     * Delete test1 and test2 users.
     * Then, the group has only test2 user.
     */
    @Test
    void delete() {
        TypedQuery query = mock(TypedQuery.class);
        Mockito.when(query.getResultList()).thenReturn(
                List.of(
                        builder(new User()).set(User::setId, 1L).set(User::setAccount, "test1").build(),
                        builder(new User()).set(User::setId, 3L).set(User::setAccount, "test3").build()
                )
        );
        final EntityManager em = MockFactory.createEntityManagerMock(query);
        final GroupUsersResource resource = new GroupUsersResource();
        final GroupUsersRequest groupUsersRequest = new GroupUsersRequest();
        groupUsersRequest.addAll(Arrays.asList("test1", "test3"));

        final List<User> users = List.of(
                builder(new User()).set(User::setId, 1L).set(User::setAccount, "test1").build(),
                builder(new User()).set(User::setId, 2L).set(User::setAccount, "test2").build(),
                builder(new User()).set(User::setId, 3L).set(User::setAccount, "test3").build()
        );
        final Group group = builder(new Group())
                .set(Group::setUsers, users)
                .build();

        resource.delete(groupUsersRequest, group, em);

        assertThat(group.getUsers()).containsExactly(
                builder(new User()).set(User::setId, 2L).set(User::setAccount, "test2").build()
        );
    }
}