package net.unit8.bouncr.web.controller.api.admin;

import enkan.collection.Parameters;
import enkan.component.doma2.DomaProvider;
import net.unit8.bouncr.web.dao.GroupDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.Group;
import net.unit8.bouncr.web.entity.User;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

/**
 * A controller about group apis.
 *
 * @author miyamoen
 */
public class GroupController {
    @Inject
    private DomaProvider daoProvider;

    @Transactional
    public int addUser(Parameters params, User user) {
        GroupDao groupDao = daoProvider.getDao(GroupDao.class);
        Group group = groupDao.selectById(params.getLong("id"));
        return groupDao.addUser(group, user);
    }

    @RolesAllowed({"LIST_GROUPS", "LIST_ANY_GROUPS"})
    public List<User> users(Parameters params) {
        GroupDao groupDao = daoProvider.getDao(GroupDao.class);
        Group group = groupDao.selectById(params.getLong("id"));

        UserDao userDao = daoProvider.getDao(UserDao.class);
        List<User> users = userDao.selectByGroupId(group.getId());

        return users;
    }
}
