package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.OAuth2Provider;
import org.seasar.doma.*;

import java.util.List;

@Dao(config = DomaConfig.class)
public interface OAuth2ProviderDao {
    @Select
    List<OAuth2Provider> selectAll();

    @Select(ensureResult = true)
    OAuth2Provider selectById(Long id);

    @Insert
    int insert(OAuth2Provider oauth2Provider);

    @Update
    int update(OAuth2Provider oauth2Provider);

    @Delete
    int delete(OAuth2Provider oauth2Provider);
}
