package net.unit8.bouncr.web.dao;

import enkan.security.UserPrincipal;
import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.Group;
import net.unit8.bouncr.web.entity.User;
import org.seasar.doma.*;
import org.seasar.doma.jdbc.SelectOptions;

import java.util.List;

/**
 * A data access object for group entity.
 *
 * @author kawasima
 */
@Dao(config = DomaConfig.class)
public interface GroupDao {
    @Select
    List<Group> selectAll();

    @Select
    List<Group> selectByPrincipalScope(UserPrincipal principal, SelectOptions selectOptions);

    @Select(ensureResult = true)
    Group selectById(Long id);

    @Select(ensureResult = true)
    Group selectByName(String name);

    @Select
    List<Group> selectByUserId(Long userId);

    @Insert
    int insert(Group group);

    @Update(sqlFile = true)
    int update(Group group, UserPrincipal principal);

    @Delete
    int delete(Group group);

    @Insert(sqlFile = true)
    int addUser(Group group, User user);

    @Delete(sqlFile = true)
    int removeUser(Group group, User user);

    @Delete(sqlFile = true)
    int clearUsers(Long groupId);
}
