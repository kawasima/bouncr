package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.Group;
import net.unit8.bouncr.web.entity.User;
import org.seasar.doma.*;

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

    @Select(ensureResult = true)
    Group selectById(Long id);

    @Select(ensureResult = true)
    Group selectByName(String name);

    @Insert
    int insert(Group group);

    @Update
    int update(Group group);

    @Delete
    int delete(Group group);

    @Insert(sqlFile = true)
    int addUser(Group group, User user);

    @Delete(sqlFile = true)
    int removeUser(Group group, User user);

    @Delete(sqlFile = true)
    int clearUsers(Long groupId);
}
