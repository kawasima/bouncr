package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.OidcApplication;
import org.seasar.doma.*;

import java.util.List;

@Dao(config= DomaConfig.class)
public interface OidcApplicationDao {
    @Select
    List<OidcApplication> selectAll();

    @Select(ensureResult = true)
    OidcApplication selectById(Long id);

    @Select(ensureResult = true)
    OidcApplication selectByClientId(String clientId);

    @Insert
    int insert(OidcApplication oauth2Application);

    @Update
    int update(OidcApplication oauth2Application);

    @Delete
    int delete(OidcApplication oauth2Application);
}
