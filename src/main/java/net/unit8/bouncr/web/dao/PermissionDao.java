package net.unit8.bouncr.web.dao;

import enkan.security.UserPrincipal;
import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.Permission;
import net.unit8.bouncr.web.entity.PermissionWithRealm;
import org.seasar.doma.*;
import org.seasar.doma.jdbc.SelectOptions;

import java.util.List;

/**
 * A data access object for permission entity.
 *
 * @author kawasima
 */
@Dao(config = DomaConfig.class)
public interface PermissionDao {
    @Select
    List<Permission> selectAll();

    @Select
    List<Permission> selectByPrincipalScope(UserPrincipal principal, SelectOptions options);

    @Select(ensureResult = true)
    Permission selectById(Long id);

    @Select
    List<PermissionWithRealm> selectByUserId(Long userId);

    @Select
    List<Permission> selectByRoleId(Long roleId);

    @Insert
    int insert(Permission permission);

    @Update
    int update(Permission permission);

    @Delete
    int delete(Permission permission);
}
