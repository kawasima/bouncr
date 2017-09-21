package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.OidcProvider;
import org.seasar.doma.*;

import java.util.List;

@Dao(config = DomaConfig.class)
public interface OidcProviderDao {
    @Select
    List<OidcProvider> selectAll();

    @Select(ensureResult = true)
    OidcProvider selectById(Long id);

    @Insert
    int insert(OidcProvider oauth2Provider);

    @Update
    int update(OidcProvider oauth2Provider);

    @Delete
    int delete(OidcProvider oauth2Provider);
}
