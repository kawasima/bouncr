package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.Realm;
import org.seasar.doma.*;

import java.util.List;

@Dao(config = DomaConfig.class)
public interface RealmDao {
    @Select
    List<Realm> selectAll();

    @Insert
    int insert(Realm realm);

    @Update
    int update(Realm realm);

    @Delete
    int delete(Realm realm);
}
