package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.Assignment;
import org.seasar.doma.Dao;
import org.seasar.doma.Delete;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;

import java.util.List;

@Dao(config = DomaConfig.class)
public interface AssignmentDao {
    @Select
    List<Assignment> selectByRealmId(Long realmId);

    @Insert
    int insert(Assignment assignment);

    @Delete
    int delete(Assignment assignment);
}
