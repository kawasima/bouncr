package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.Permission;
import net.unit8.bouncr.web.entity.Role;
import net.unit8.bouncr.web.entity.RolePermission;
import org.seasar.doma.*;

import java.util.List;

/**
 * A data access object for role entity.
 *
 * @author kawasima
 */
@Dao(config = DomaConfig.class)
public interface RoleDao {
    @Select
    List<Role> selectAll();

    @Select(ensureResult = true)
    Role selectById(Long id);

    @Insert(sqlFile = true)
    int addPermission(Role role, Permission permission);

    @Delete(sqlFile = true)
    int clearPermissions(Role role);

    @Insert
    int insert(Role role);

    @Update
    int update(Role role);

    @Delete
    int delete(Role role);
}
