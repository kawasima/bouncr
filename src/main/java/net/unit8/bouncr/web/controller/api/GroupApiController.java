package net.unit8.bouncr.web.controller.api;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.security.UserPrincipal;
import net.unit8.bouncr.web.dao.GroupDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.Group;
import net.unit8.bouncr.web.entity.User;
import org.seasar.doma.jdbc.SelectOptions;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A controller about group actions.
 *
 * @author miyamoen
 */
public class GroupApiController {
    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @RolesAllowed({"LIST_GROUPS", "LIST_ANY_GROUPS"})
    public List<User> users(UserPrincipal principal, Parameters params) {
        GroupDao groupDao = daoProvider.getDao(GroupDao.class);
        Group group = groupDao.selectById(params.getLong("id"));

        UserDao userDao = daoProvider.getDao(UserDao.class);
        List<User> users = userDao.selectByGroupId(group.getId());

        return users;
    }

}
