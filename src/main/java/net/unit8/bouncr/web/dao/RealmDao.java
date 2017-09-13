package net.unit8.bouncr.web.dao;

import enkan.security.UserPrincipal;
import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.Realm;
import org.seasar.doma.*;
import org.seasar.doma.jdbc.SelectOptions;

import java.util.List;

@Dao(config = DomaConfig.class)
public interface RealmDao {
    @Select
    List<Realm> selectAll();

    @Select
    List<Realm> selectByApplicationId(Long applicationId, UserPrincipal principal, SelectOptions options);

    @Select(ensureResult = true)
    Realm selectById(Long id);

    @Insert
    int insert(Realm realm);

    @Update
    int update(Realm realm);

    @Delete
    int delete(Realm realm);
}
