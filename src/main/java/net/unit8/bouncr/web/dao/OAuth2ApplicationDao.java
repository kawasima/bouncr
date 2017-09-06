package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.entity.OAuth2Application;
import org.seasar.doma.*;

import java.util.List;

@Dao
public interface OAuth2ApplicationDao {
    @Select
    List<OAuth2Application> selectAll();

    @Select(ensureResult = true)
    OAuth2Application selectById(Long id);

    @Select
    OAuth2Application selectByClientId(String clientId);

    @Insert
    int insert(OAuth2Application oauth2Application);

    @Update
    int update(OAuth2Application oauth2Application);

    @Delete
    int delete(OAuth2Application oauth2Application);
}
