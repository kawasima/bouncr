package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.Role;
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

    @Insert
    int insert(Role role);

    @Update
    int update(Role role);

    @Delete
    int delete(Role role);
}
