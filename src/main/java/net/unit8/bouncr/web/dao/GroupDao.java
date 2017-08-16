package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.Group;
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

    @Insert
    int insert(Group group);

    @Update
    int update(Group group);

    @Delete
    int delete(Group group);
}
