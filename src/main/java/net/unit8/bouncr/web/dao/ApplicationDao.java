package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.Application;
import org.seasar.doma.*;

import java.util.List;

/**
 * A data access object for application entity.
 *
 * @author kawasima
 */
@Dao(config = DomaConfig.class)
public interface ApplicationDao {
    @Select
    List<Application> selectAll();

    @Select
    List<Application> selectByUserId(Long userId);

    @Select
    Application selectById(Long id);

    @Insert
    int insert(Application app);

    @Update
    int update(Application app);

    @Delete
    int delete(Application app);
}
